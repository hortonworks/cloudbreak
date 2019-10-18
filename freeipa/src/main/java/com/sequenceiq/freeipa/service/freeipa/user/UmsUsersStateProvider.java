package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState.Builder;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@Service
public class UmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProvider.class);

    private static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public Map<String, UsersState> getEnvToUmsUsersStateMap(String accountId, String actorCrn, Set<String> environmentCrns,
                                                            Set<String> userCrns, Set<String> machineUserCrns) {
        try {
            final Optional<String> requestIdOptional = Optional.of(
                Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()))
                    .orElseGet(() -> {
                String requestId = UUID.randomUUID().toString();
                LOGGER.debug("No requestId found. Setting request id to new UUID [{}]", requestId);
                return requestId;
            }));

            LOGGER.debug("Getting UMS state for environments {} with requestId {}", environmentCrns, requestIdOptional);

            Map<String, UsersState> envUsersStateMap = new HashMap<>();

            boolean fullSync = userCrns.isEmpty() && machineUserCrns.isEmpty();

            List<User> users = getUsers(actorCrn, accountId, requestIdOptional, fullSync, userCrns);

            List<MachineUser> machineUsers = getMachineUsers(actorCrn, accountId, requestIdOptional, fullSync, machineUserCrns);


            Map<String, FmsGroup> crnToFmsGroup = grpcUmsClient.listGroups(actorCrn, accountId, List.of(), requestIdOptional).stream()
                    .collect(Collectors.toMap(Group::getCrn, this::umsGroupToGroup));

            environmentCrns.stream().forEach(environmentCrn -> {
                Builder userStateBuilder = new Builder();

                crnToFmsGroup.values().stream().forEach(userStateBuilder::addGroup);

                // add internal usersync group for each environment
                FmsGroup internalUserSyncGroup = new FmsGroup();
                internalUserSyncGroup.setName(UserServiceConstants.CDP_USERSYNC_INTERNAL_GROUP);
                userStateBuilder.addGroup(internalUserSyncGroup);

                users.stream().forEach(u -> {
                    FmsUser fmsUser = umsUserToUser(u);
                    // add workload username for each user. This will be helpful in getting users from IPA.
                    userStateBuilder.addRequestedWorkloadUsers(fmsUser);

                    handleUser(userStateBuilder, crnToFmsGroup, actorCrn, u.getCrn(), fmsUser, environmentCrn, requestIdOptional);

                });

                machineUsers.stream().forEach(mu -> {
                    FmsUser fmsUser = umsMachineUserToUser(mu);
                    userStateBuilder.addRequestedWorkloadUsers(fmsUser);
                    // add workload username for each user. This will be helpful in getting users from IPA.

                    handleUser(userStateBuilder, crnToFmsGroup, actorCrn, mu.getCrn(), fmsUser, environmentCrn, requestIdOptional);
                });

                envUsersStateMap.put(environmentCrn, userStateBuilder.build());
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
        return new WorkloadCredential(hashedPassword, keys);
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

    private void handleUser(Builder userStateBuilder, Map<String, FmsGroup> crnToFmsGroup,
                            String actorCrn, String memberCrn, FmsUser fmsUser, String environmentCrn, Optional<String> requestId) {

        GetRightsResponse rightsResponse = grpcUmsClient.getRightsForUser(actorCrn, memberCrn, environmentCrn, requestId);
        if (isEnvironmentUser(environmentCrn, rightsResponse)) {
            userStateBuilder.addUser(fmsUser);
            rightsResponse.getGroupCrnList().stream().forEach(gcrn -> {
                userStateBuilder.addMemberToGroup(crnToFmsGroup.get(gcrn).getName(), fmsUser.getName());
            });

            // Since this user is eligible, add this user to internal group
            userStateBuilder.addMemberToGroup(UserServiceConstants.CDP_USERSYNC_INTERNAL_GROUP, fmsUser.getName());

            List<String> workloadAdministrationGroupNames = rightsResponse.getWorkloadAdministrationGroupNameList();
            LOGGER.debug("workloadAdministrationGroupNameList = {}", workloadAdministrationGroupNames);
            workloadAdministrationGroupNames.forEach(groupName -> {
                userStateBuilder.addGroup(nameToGroup(groupName));
                userStateBuilder.addMemberToGroup(groupName, fmsUser.getName());
            });

            if (isEnvironmentAdmin(environmentCrn, rightsResponse)) {
                // TODO: introduce a flag for adding admin
                userStateBuilder.addMemberToGroup("admins", fmsUser.getName());
            }

            // get credentials
            userStateBuilder.addWorkloadCredentials(fmsUser.getName(), getCredentials(memberCrn, requestId));
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
