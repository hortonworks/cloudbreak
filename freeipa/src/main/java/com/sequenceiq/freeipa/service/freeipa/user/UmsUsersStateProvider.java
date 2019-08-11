package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Role;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RoleAssignment;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsState.Builder;

@Service
public class UmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProvider.class);

    @Inject
    private GrpcUmsClient umsClient;

    public Map<String, UmsState> getEnvToUmsStateMap(String accountId, String actorCrn, Set<String> environmentsFilter,
                Set<String> userCrns, Set<String> machineUserCrns) {
        if (environmentsFilter == null || environmentsFilter.size() == 0) {
            LOGGER.error("Environment Filter argument is null of empty");
            throw new RuntimeException("Environment Filter argument is null of empty");
        }

        try {
            List<User> users = userCrns.isEmpty() ? umsClient.listAllUsers(actorCrn, accountId, Optional.empty())
                    : umsClient.listUsers(actorCrn, accountId, List.copyOf(userCrns), Optional.empty());
            List<MachineUser> machineUsers = machineUserCrns.isEmpty() ? umsClient.listAllMachineUsers(actorCrn, accountId, Optional.empty())
                    : umsClient.listMachineUsers(actorCrn, accountId, List.copyOf(machineUserCrns), Optional.empty());

            // TODO: No need to fetch groups
            List<Group> groups = umsClient.listAllGroups(actorCrn, accountId, Optional.empty());
            // TODO: existing code still fetched rights to calculate groups when using user filter. Can we get rid of that too?
//            } else {
//                // filter for set of users
//                Set<String> groupCrns = new HashSet<>();
//                users = umsClient.listUsers(actorCrn, accountId, List.copyOf(userCrns), Optional.empty());
//                users.forEach(u -> {
//                    // TODO: No need of Rights call here
//                    GetRightsResponse rights = umsClient.getRightsForUser(actorCrn, u.getCrn(), null, Optional.empty());
//                    // below line is used in common private method
//                    //umsStateBuilder.addUser(u, rights);
//                    groupCrns.addAll(rights.getGroupCrnList());
//                });
//
//                machineUsers = new ArrayList<>();
//                groups = umsClient.listGroups(actorCrn, accountId, List.copyOf(groupCrns), Optional.empty());
//            }
            return getEnvToUmsStateMap(accountId, actorCrn, environmentsFilter, users, machineUsers, groups);

        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: %s", e.getMessage()));
        }
    }

    private Map<String, UmsState> getEnvToUmsStateMap(
        String accountId, String actorCrn, Set<String> environmentsFilter, List<User> users, List<MachineUser> machineUsers, List<Group> groups) {
        UmsState.Builder umsStateBuilder = new UmsState.Builder();

        // process each user and update environmentCRN -> UmsState map
        Map<String, UmsState> envUmsStateMap = new HashMap<>();

        environmentsFilter.stream().forEach(envCRN -> {
            processForEnvironmentRights(umsStateBuilder, actorCrn, envCRN, users, machineUsers);
            envUmsStateMap.put(envCRN, umsStateBuilder.build());
        });

        // get all groups for users
        Map<User, List<Group>> usersToGroupsMap = umsClient.getUsersToGroupsMap(actorCrn, accountId, users, Optional.empty());
        umsStateBuilder.addUserToGroupMap(usersToGroupsMap);
        //groups.stream().forEach(g -> umsStateBuilder.addGroup(g));

        return envUmsStateMap;
    }

    private void processForEnvironmentRights(
        Builder umsStateBuilder, String actorCrn, String envCRN, List<User> allUsers, List<MachineUser> allMachineUsers) {
        // for all users, check right for the passed envCRN
        for (User u : allUsers) {

            if (umsClient.checkRight(actorCrn, u.getCrn(), "environments/write", envCRN, Optional.empty())) {
                // if (true) {
                umsStateBuilder.addUser(u, null);
            } else {
                // TODO: check for power user by getting roles.
                GetRightsResponse rights = umsClient.getRightsForUser(actorCrn, u.getCrn(), envCRN, Optional.empty());
                List<RoleAssignment> roles = rights.getRoleAssignmentList();
                if (roles == null || roles.size() > 0) {
                    for (RoleAssignment roleAssignment : roles) {
                        Role role = roleAssignment.getRole();
                        // TODO: this is hard coded, need to get from ROLES ENUM
                        if (role.getCrn().toLowerCase().contains("Power".toLowerCase())) {
                            // power userx
                            umsStateBuilder.addAdminUser(u);
                            umsStateBuilder.addUser(u, null);
                            break;
                        }
                    }

                }
            }
        }


        // machine users
        for (MachineUser machineUser : allMachineUsers) {

            // TODO: Remove commented code
//            GetRightsResponse rights = umsClient.getRightsForUser(actorCrn, machineUser.getCrn(), envCRN, Optional.empty());
//            // check if user has right for this env
//            List<ResourceRoleAssignment> assignedResourceRoles = rights.getResourceRolesAssignmentList();
//            if (rights.getThunderheadAdmin()) {
//                umsStateBuilder.addAdminUser(u);
//                continue;
//
//            }
//
//            if (assignedResourceRoles == null || assignedResourceRoles.size() == 0) {
//                continue;
//            }

            // Machine User can be a power user also
            if (umsClient.checkRight(actorCrn, machineUser.getCrn(), "environments/setPassword", envCRN, Optional.empty())) {
                // this is admin user having write access
                umsStateBuilder.addMachineUser(machineUser, null);
            }
        }

    }
}
