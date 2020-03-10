package com.sequenceiq.freeipa.service.freeipa.user;

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

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ResourceRoleAssignment;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RoleAssignment;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
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

            Set<FmsGroup> wags = grpcUmsClient.listWorkloadAdministrationGroups(IAM_INTERNAL_ACTOR_CRN, accountId, requestIdOptional).stream()
                    .map(wag -> nameToGroup(wag.getWorkloadAdministrationGroupName()))
                    .collect(Collectors.toSet());

            environmentCrns.stream().forEach(environmentCrn -> {
                UmsUsersState.Builder umsUsersStateBuilder = new UmsUsersState.Builder();
                UsersState.Builder usersStateBuilder = new UsersState.Builder();

                crnToFmsGroup.values().stream().forEach(usersStateBuilder::addGroup);
                wags.stream().forEach(usersStateBuilder::addGroup);

                // add internal usersync group for each environment
                FmsGroup internalUserSyncGroup = new FmsGroup();
                internalUserSyncGroup.setName(UserServiceConstants.CDP_USERSYNC_INTERNAL_GROUP);
                usersStateBuilder.addGroup(internalUserSyncGroup);

                users.stream().forEach(u -> {
                    FmsUser fmsUser = umsUserToUser(u);
                    // add workload username for each user. This will be helpful in getting users from IPA.
                    umsUsersStateBuilder.addRequestedWorkloadUsers(fmsUser);

                    handleUser(umsUsersStateBuilder, usersStateBuilder, crnToFmsGroup, actorCrn, u.getCrn(), fmsUser, environmentCrn, requestIdOptional);

                });

                machineUsers.stream().forEach(mu -> {
                    FmsUser fmsUser = umsMachineUserToUser(mu);
                    umsUsersStateBuilder.addRequestedWorkloadUsers(fmsUser);
                    // add workload username for each user. This will be helpful in getting users from IPA.

                    handleUser(umsUsersStateBuilder, usersStateBuilder, crnToFmsGroup, actorCrn, mu.getCrn(), fmsUser, environmentCrn, requestIdOptional);
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

    private boolean isEnvironmentUser(String enviromentCrn, GetRightsResponse rightsResponse) {

        List<RoleAssignment> rolesAssignedList = rightsResponse.getRoleAssignmentList();
        for (RoleAssignment roleAssigned : rolesAssignedList) {
            // TODO: should come from IAM Roles and check against Role Object
            if (roleAssigned.getRole().getCrn().contains("PowerUser") ||
                roleAssigned.getRole().getCrn().contains("EnvironmentAdmin")) {
                return true;
                // admins are also users
            }
        }

        List<ResourceRoleAssignment> resourceRoleAssignedList = rightsResponse.getResourceRolesAssignmentList();
        for (ResourceRoleAssignment resourceRoleAssigned : resourceRoleAssignedList) {
            // TODO: should come from IAM Roles and check against Role Object
            if (resourceRoleAssigned.getResourceRole().getCrn().contains("EnvironmentAdmin") ||
                (resourceRoleAssigned.getResourceRole().getCrn().contains("EnvironmentUser"))) {
                return true;
            }
        }

        return false;
    }

    private boolean isEnvironmentAdmin(String enviromentCrn, GetRightsResponse rightsResponse) {
        List<RoleAssignment> rolesAssignedList = rightsResponse.getRoleAssignmentList();
        for (RoleAssignment roleAssigned : rolesAssignedList) {
            // TODO: should come from IAM Roles and check against Role Object
            if (roleAssigned.getRole().getCrn().contains("PowerUser") ||
                roleAssigned.getRole().getCrn().contains("EnvironmentAdmin")) {
                return true;
            }
        }

        List<ResourceRoleAssignment> resourceRoleAssignedList = rightsResponse.getResourceRolesAssignmentList();
        for (ResourceRoleAssignment resourceRoleAssigned : resourceRoleAssignedList) {
            // TODO: should come from IAM Roles and check against Role Object
            if (resourceRoleAssigned.getResourceRole().getCrn().contains("EnvironmentAdmin")) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("ParameterNumber")
    private void handleUser(UmsUsersState.Builder umsUsersStateBuilder, UsersState.Builder usersStateBuilder, Map<String, FmsGroup> crnToFmsGroup,
                            String actorCrn, String memberCrn, FmsUser fmsUser, String environmentCrn, Optional<String> requestId) {
        try {
            GetRightsResponse rightsResponse = grpcUmsClient.getRightsForUser(actorCrn, memberCrn, environmentCrn, requestId);
            if (isEnvironmentUser(environmentCrn, rightsResponse)) {
                usersStateBuilder.addUser(fmsUser);
                rightsResponse.getGroupCrnList().stream().forEach(gcrn -> {
                    FmsGroup group = crnToFmsGroup.get(gcrn);
                    // If the group is null, then there has been a group membership change after we started the sync
                    // the group and group membership will be updated on the next sync
                    if (group != null) {
                        usersStateBuilder.addMemberToGroup(group.getName(), fmsUser.getName());
                    }
                });

                // Since this user is eligible, add this user to internal group
                usersStateBuilder.addMemberToGroup(UserServiceConstants.CDP_USERSYNC_INTERNAL_GROUP, fmsUser.getName());

                List<String> workloadAdministrationGroupNames = rightsResponse.getWorkloadAdministrationGroupNameList();
                LOGGER.debug("workloadAdministrationGroupNameList = {}", workloadAdministrationGroupNames);
                workloadAdministrationGroupNames.forEach(groupName -> {
                    usersStateBuilder.addGroup(nameToGroup(groupName));
                    usersStateBuilder.addMemberToGroup(groupName, fmsUser.getName());
                });

                if (isEnvironmentAdmin(environmentCrn, rightsResponse)) {
                    // TODO: introduce a flag for adding admin
                    usersStateBuilder.addMemberToGroup("admins", fmsUser.getName());
                }

                // get credentials
                umsUsersStateBuilder.addWorkloadCredentials(fmsUser.getName(), getCredentials(memberCrn, requestId));
            }
        } catch (StatusRuntimeException e) {
            // NOT_FOUND errors indicate that a user/machineUser has been deleted after we have
            // retrieved the list of users/machineUsers from the UMS. Treat these users as if
            // they do not have the right to access this environment and belong to no groups.
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                LOGGER.warn("CRN {} not found in UMS. Treating as if CRN has no rights to environment {}: {}",
                        memberCrn, environmentCrn, e.getLocalizedMessage());
            } else {
                throw e;
            }
        }
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

}
