package com.sequenceiq.freeipa.service.freeipa.user;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Service
public class UmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProvider.class);

    private static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private static final String ADMIN_FREEIPA_GROUP = "admins";

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public Map<String, UmsUsersState> getEnvToUmsUsersStateMap(String accountId, String actorCrn, Set<String> environmentCrns,
                                                            Set<String> userCrns, Set<String> machineUserCrns, Optional<String> requestIdOptional) {
        try {
            LOGGER.debug("Getting UMS state for environments {} with requestId {}", environmentCrns, requestIdOptional);

            Map<String, UmsUsersState> envUsersStateMap = new HashMap<>();

            boolean fullSync = userCrns.isEmpty() && machineUserCrns.isEmpty();

            List<User> users = getUsers(actorCrn, accountId, requestIdOptional, fullSync, userCrns);

            List<MachineUser> machineUsers = getMachineUsers(actorCrn, accountId, requestIdOptional, fullSync, machineUserCrns);

            Map<String, FmsGroup> crnToFmsGroup = grpcUmsClient.listGroups(actorCrn, accountId, List.of(), requestIdOptional).stream()
                    .collect(Collectors.toMap(Group::getCrn, this::umsGroupToGroup));

            environmentCrns.stream().forEach(environmentCrn -> {
                UmsUsersState.Builder umsUsersStateBuilder = new UmsUsersState.Builder();
                UsersState.Builder usersStateBuilder = new UsersState.Builder();

                crnToFmsGroup.values().stream().forEach(usersStateBuilder::addGroup);

                // add internal usersync group for each environment
                FmsGroup internalUserSyncGroup = new FmsGroup();
                internalUserSyncGroup.setName(UserServiceConstants.CDP_USERSYNC_INTERNAL_GROUP);
                usersStateBuilder.addGroup(internalUserSyncGroup);

                EnvironmentAccessChecker environmentAccessChecker = createEnvironmentAccessChecker(environmentCrn);

                users.stream().forEach(u -> {
                    FmsUser fmsUser = umsUserToUser(u);
                    // add workload username for each user. This will be helpful in getting users from IPA.
                    umsUsersStateBuilder.addRequestedWorkloadUsers(fmsUser);

                    handleUser(umsUsersStateBuilder, usersStateBuilder, crnToFmsGroup, u.getCrn(), fmsUser,
                            environmentAccessChecker.hasAccess(u.getCrn(), requestIdOptional), requestIdOptional);

                });

                machineUsers.stream().forEach(mu -> {
                    FmsUser fmsUser = umsMachineUserToUser(mu);
                    umsUsersStateBuilder.addRequestedWorkloadUsers(fmsUser);
                    // add workload username for each user. This will be helpful in getting users from IPA.

                    handleUser(umsUsersStateBuilder, usersStateBuilder, crnToFmsGroup, mu.getCrn(), fmsUser,
                            environmentAccessChecker.hasAccess(mu.getCrn(), requestIdOptional), requestIdOptional);
                });

                umsUsersStateBuilder.setUsersState(usersStateBuilder.build());
                envUsersStateMap.put(environmentCrn, umsUsersStateBuilder.build());
            });

            return envUsersStateMap;
        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: '%s'", e.getLocalizedMessage()), e);
        }
    }

    private List<User> getUsers(String actorCrn, String accountId, Optional<String> requestIdOptional, boolean fullSync, Set<String> userCrns) {
        if (fullSync) {
            return grpcUmsClient.listAllUsers(actorCrn, accountId, requestIdOptional);
        } else if (!userCrns.isEmpty()) {
            return grpcUmsClient.listUsers(actorCrn, accountId, List.copyOf(userCrns), requestIdOptional);
        } else {
            return List.of();
        }
    }

    private List<MachineUser> getMachineUsers(String actorCrn, String accountId, Optional<String> requestIdOptional,
            boolean fullSync, Set<String> machineUserCrns) {
        if (fullSync) {
            return grpcUmsClient.listAllMachineUsers(actorCrn, accountId, requestIdOptional);
        } else if (!machineUserCrns.isEmpty()) {
            return grpcUmsClient.listMachineUsers(actorCrn, accountId, List.copyOf(machineUserCrns), requestIdOptional);
        } else {
            return List.of();
        }
    }

    private WorkloadCredential getCredentials(String userCrn, Optional<String> requestId) {
        GetActorWorkloadCredentialsResponse response = grpcUmsClient.getActorWorkloadCredentials(IAM_INTERNAL_ACTOR_CRN, userCrn, requestId);
        String hashedPassword = response.getPasswordHash();
        List<ActorKerberosKey> keys = response.getKerberosKeysList();
        long expirationDate = response.getPasswordHashExpirationDate();
        Optional<Instant> expirationInstant = expirationDate == 0 ? Optional.empty() : Optional.of(Instant.ofEpochMilli(expirationDate));

        return new WorkloadCredential(hashedPassword, keys, expirationInstant);
    }

    @SuppressWarnings("ParameterNumber")
    private void handleUser(UmsUsersState.Builder umsUsersStateBuilder, UsersState.Builder usersStateBuilder, Map<String, FmsGroup> crnToFmsGroup,
                            String memberCrn, FmsUser fmsUser, EnvironmentAccessRights environmentAccessRights, Optional<String> requestId) {
        try {
            if (environmentAccessRights.hasEnvironmentAccessRight()) {
                String username = fmsUser.getName();
                String accountId = Crn.safeFromString(memberCrn).getAccountId();

                // Retrieve all information from UMS before modifying to the UmsUsersState or UsersState. This is so that
                // we don't partially modify the state if the member has been deleted after we started the sync
                List<String> groupCrnsForMember = grpcUmsClient.listGroupsForMember(IAM_INTERNAL_ACTOR_CRN, accountId, memberCrn, requestId);
                UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse workloadAdministrationGroups =
                        grpcUmsClient.listWorkloadAdministrationGroupsForMember(IAM_INTERNAL_ACTOR_CRN, memberCrn, requestId);
                WorkloadCredential workloadCredential = getCredentials(memberCrn, requestId);

                groupCrnsForMember.forEach(gcrn -> {
                    FmsGroup group = crnToFmsGroup.get(gcrn);
                    // If the group is null, then there has been a group membership change after we started the sync
                    // the group and group membership will be updated on the next sync
                    if (group != null) {
                        usersStateBuilder.addMemberToGroup(group.getName(), username);
                    } else {
                        LOGGER.warn("{} is a member of unexpected group {}. Group must have been added after UMS state calculation started",
                                memberCrn, gcrn);
                    }
                });

                workloadAdministrationGroups.getWorkloadAdministrationGroupNameList().forEach(groupName -> {
                    usersStateBuilder.addGroup(nameToGroup(groupName));
                    usersStateBuilder.addMemberToGroup(groupName, username);
                });

                if (environmentAccessRights.hasAdminFreeIPARight()) {
                    usersStateBuilder.addMemberToGroup(ADMIN_FREEIPA_GROUP, username);
                }

                addMemberToInternalTrackingGroup(usersStateBuilder, username);

                umsUsersStateBuilder.addWorkloadCredentials(fmsUser.getName(), workloadCredential);

                usersStateBuilder.addUser(fmsUser);
            }
        } catch (StatusRuntimeException e) {
            // NOT_FOUND errors indicate that a user/machineUser has been deleted after we have
            // retrieved the list of users/machineUsers from the UMS. Interrupt calculation of group
            // membership.
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                LOGGER.warn("Member CRN {} not found in UMS. Member will not be added to the UMS Users State. {}",
                        memberCrn, e.getLocalizedMessage());
            } else {
                throw e;
            }
        }

    }

    private void addMemberToInternalTrackingGroup(UsersState.Builder usersStateBuilder, String username) {
        usersStateBuilder.addMemberToGroup(UserServiceConstants.CDP_USERSYNC_INTERNAL_GROUP, username);
    }

    private FmsUser umsUserToUser(User umsUser) {
        FmsUser fmsUser = new FmsUser();
        fmsUser.setName(umsUser.getWorkloadUsername());
        fmsUser.setFirstName(getOrDefault(umsUser.getFirstName(), "None"));
        fmsUser.setLastName(getOrDefault(umsUser.getLastName(), "None"));
        return fmsUser;
    }

    private FmsGroup nameToGroup(String name) {
        FmsGroup fmsGroup = new FmsGroup();
        fmsGroup.setName(name);
        return fmsGroup;
    }

    private String getOrDefault(String value, String other) {
        return (value == null || value.isBlank()) ? other : value;
    }

    private FmsUser umsMachineUserToUser(MachineUser umsMachineUser) {
        FmsUser fmsUser = new FmsUser();
        fmsUser.setName(umsMachineUser.getWorkloadUsername());
        // TODO what should the appropriate first and last name be for machine users?
        fmsUser.setFirstName("Machine");
        fmsUser.setLastName("User");
        return fmsUser;
    }

    private FmsGroup umsGroupToGroup(Group umsGroup) {
        FmsGroup fmsGroup = new FmsGroup();
        fmsGroup.setName(umsGroup.getGroupName());
        return fmsGroup;
    }

    private EnvironmentAccessChecker createEnvironmentAccessChecker(String environmentCrn) {
        requireNonNull(environmentCrn, "environmentCrn is null");
        return new EnvironmentAccessChecker(grpcUmsClient, environmentCrn);
    }
}
