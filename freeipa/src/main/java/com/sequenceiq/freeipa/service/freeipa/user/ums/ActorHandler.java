package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsGroupConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

public class ActorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActorHandler.class);

    private FmsGroupConverter fmsGroupConverter;

    private UmsUsersState.Builder umsUsersStateBuilder;

    private UsersState.Builder usersStateBuilder;

    private Map<String, FmsGroup> crnToFmsGroup;

    private ActorHandler(
            FmsGroupConverter fmsGroupConverter,
            UmsUsersState.Builder umsUsersStateBuilder,
            UsersState.Builder usersStateBuilder,
            Map<String, FmsGroup> crnToFmsGroup) {
        this.fmsGroupConverter = requireNonNull(fmsGroupConverter);
        this.umsUsersStateBuilder = requireNonNull(umsUsersStateBuilder);
        this.usersStateBuilder = requireNonNull(usersStateBuilder);
        this.crnToFmsGroup = requireNonNull(crnToFmsGroup);
    }

    public void handleActor(
            UserManagementProto.RightsCheckResult rightsCheckResult,
            FmsUser fmsUser,
            String actorCrn,
            Supplier<Collection<String>> groupCrnMembershipSupplier,
            Supplier<Collection<String>> wagMembershipSupplier,
            Supplier<WorkloadCredential> workloadCredentialSupplier,
            List<UserManagementProto.CloudIdentity> cloudIdentityList) {

        boolean hasEnvironmentAccess = rightsCheckResult.getHasRight(1);
        boolean freeipaAdmin = rightsCheckResult.getHasRight(0);
        if (hasEnvironmentAccess) {
            String workloadUsername = fmsUser.getName();

            // Retrieve all information from UMS before modifying to the UmsUsersState or UsersState. This is so that
            // we don't partially modify the state if the member has been deleted after we started the sync
            Collection<String> groupCrnsForMember = groupCrnMembershipSupplier.get();
            Collection<String> workloadAdministrationGroupsForMember = wagMembershipSupplier.get();
            WorkloadCredential workloadCredential = workloadCredentialSupplier.get();

            groupCrnsForMember.forEach(gcrn -> {
                FmsGroup group = crnToFmsGroup.get(gcrn);
                // If the group is null, then there has been a group membership change after we started the sync
                // the group and group membership will be updated on the next sync
                if (group != null) {
                    usersStateBuilder.addMemberToGroup(group.getName(), workloadUsername);
                } else {
                    LOGGER.warn("{} is a member of unexpected group {}. Group must have been added after UMS state calculation started",
                            workloadUsername, gcrn);
                }
            });
            workloadAdministrationGroupsForMember.stream()
                    .forEach(wagName -> {
                        usersStateBuilder.addGroup(fmsGroupConverter.nameToGroup(wagName));
                        usersStateBuilder.addMemberToGroup(wagName, workloadUsername);
                    });

            addMemberToInternalTrackingGroup(usersStateBuilder, workloadUsername);
            if (freeipaAdmin) {
                usersStateBuilder.addMemberToGroup(UserSyncConstants.ADMINS_GROUP, workloadUsername);
            }

            umsUsersStateBuilder.addWorkloadCredentials(workloadUsername, workloadCredential);
            umsUsersStateBuilder.addUserCloudIdentities(workloadUsername, cloudIdentityList);
            usersStateBuilder.addUserMetadata(workloadUsername, new UserMetadata(actorCrn, workloadCredential.getVersion()));
            usersStateBuilder.addUser(fmsUser);
        }
    }

    private void addMemberToInternalTrackingGroup(UsersState.Builder usersStateBuilder, String username) {
        usersStateBuilder.addMemberToGroup(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP, username);
    }

    public static ActorHandler.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private FmsGroupConverter fmsGroupConverter;

        private UmsUsersState.Builder umsUsersStateBuilder;

        private UsersState.Builder usersStateBuilder;

        private Map<String, FmsGroup> crnToFmsGroup;

        public Builder withFmsGroupConverter(FmsGroupConverter fmsGroupConverter) {
            this.fmsGroupConverter = requireNonNull(fmsGroupConverter);
            return this;
        }

        public Builder withUmsUsersStateBuilder(UmsUsersState.Builder umsUsersStateBuilder) {
            this.umsUsersStateBuilder = requireNonNull(umsUsersStateBuilder);
            return this;
        }

        public Builder withUsersStateBuilder(UsersState.Builder usersStateBuilder) {
            this.usersStateBuilder = requireNonNull(usersStateBuilder);
            return this;
        }

        public Builder withCrnToFmsGroup(Map<String, FmsGroup> crnToFmsGroup) {
            this.crnToFmsGroup = requireNonNull(crnToFmsGroup);
            return this;
        }

        public ActorHandler build() {
            return new ActorHandler(
                    fmsGroupConverter,
                    umsUsersStateBuilder,
                    usersStateBuilder,
                    crnToFmsGroup);
        }
    }
}
