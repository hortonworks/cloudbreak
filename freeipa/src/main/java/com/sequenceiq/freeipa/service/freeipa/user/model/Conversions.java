package com.sequenceiq.freeipa.service.freeipa.user.model;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import java.time.Instant;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

public class Conversions {

    @VisibleForTesting
    static final String NONE_STRING = "None";

    private Conversions() {
    }

    public static FmsUser toFmsUser(UserManagementProto.User umsUser) {
        return createFmsUser(umsUser.getWorkloadUsername(),
                umsUser.getFirstName(),
                umsUser.getLastName());
    }

    public static FmsUser toFmsUser(UserManagementProto.MachineUser umsMachineUser) {
        // Machine users don't have a first and last name.
        // Store the machine user name and id instead.
        return createFmsUser(umsMachineUser.getWorkloadUsername(),
                umsMachineUser.getMachineUserName(),
                umsMachineUser.getMachineUserId());
    }

    public static FmsUser toFmsUser(
            UserManagementProto.UserSyncActorDetails actorDetails) {
        return createFmsUser(actorDetails.getWorkloadUsername(),
                actorDetails.getFirstName(),
                actorDetails.getLastName());
    }

    private static FmsUser createFmsUser(String workloadUsername, String firstName, String lastName) {
        checkArgument(!Strings.isNullOrEmpty(workloadUsername));
        FmsUser fmsUser = new FmsUser();
        fmsUser.withName(workloadUsername);
        fmsUser.withFirstName(orDefault(firstName, NONE_STRING));
        fmsUser.withLastName(orDefault(lastName, NONE_STRING));
        return fmsUser;
    }

    public static FmsGroup umsGroupToGroup(UserManagementProto.Group umsGroup) {
        return nameToGroup(umsGroup.getGroupName());
    }

    public static FmsGroup nameToGroup(String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        FmsGroup fmsGroup = new FmsGroup();
        fmsGroup.withName(name);
        return fmsGroup;
    }

    private static String orDefault(String value, String other) {
        return (value == null || value.isBlank()) ? other : value;
    }

    public static WorkloadCredential toWorkloadCredential(
            UserManagementProto.ActorWorkloadCredentials actorWorkloadCredentials) {
        return new WorkloadCredential(actorWorkloadCredentials.getPasswordHash(),
                actorWorkloadCredentials.getKerberosKeysList(),
                toOptionalInstant(actorWorkloadCredentials.getPasswordHashExpirationDate()),
                actorWorkloadCredentials.getSshPublicKeyList());
    }

    public static WorkloadCredential toWorkloadCredential(
            UserManagementProto.GetActorWorkloadCredentialsResponse actorWorkloadCredentials) {
        return new WorkloadCredential(actorWorkloadCredentials.getPasswordHash(),
                actorWorkloadCredentials.getKerberosKeysList(),
                toOptionalInstant(actorWorkloadCredentials.getPasswordHashExpirationDate()),
                actorWorkloadCredentials.getSshPublicKeyList());
    }

    private static Optional<Instant> toOptionalInstant(long epochMillis) {
        return epochMillis == 0 ?
                Optional.empty() : Optional.of(Instant.ofEpochMilli(epochMillis));
    }
}
