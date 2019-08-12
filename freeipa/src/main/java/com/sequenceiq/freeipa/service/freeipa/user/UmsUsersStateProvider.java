package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProvider.class);

    @Inject
    private GrpcUmsClient umsClient;

    private final String environmentWrite = "environments/write";

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

            return getEnvToUmsStateMap(accountId, actorCrn, environmentsFilter, users, machineUsers);

        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: %s", e.getMessage()));
        }
    }

    private Map<String, UmsState> getEnvToUmsStateMap(
        String accountId, String actorCrn, Set<String> environmentsFilter, List<User> users, List<MachineUser> machineUsers) {
        UmsState.Builder umsStateBuilder = new UmsState.Builder();

        // process each user and update environmentCRN -> UmsState map
        Map<String, UmsState> envUmsStateMap = new HashMap<>();

        environmentsFilter.stream().forEach(envCRN -> {
            List<User> rightfulUsers = getUsersWithEnvironmentRights(actorCrn, envCRN, users);
            umsStateBuilder.addUsers(rightfulUsers);

            List<MachineUser> rightfulMachineUsers = getMachineUsersWithEnvironmentRights(actorCrn, envCRN, machineUsers);
            umsStateBuilder.addMachineUsers(rightfulMachineUsers);

            // get all groups for identified users those having rights.
            Map<User, List<Group>> usersToGroupsMap = umsClient.getUsersToGroupsMap(actorCrn, accountId, rightfulUsers, Optional.empty());
            umsStateBuilder.addUserToGroupMap(usersToGroupsMap);

            Map<MachineUser, List<Group>> machineUsersToGroupsMap = umsClient.getMachineUsersToGroupsMap(actorCrn, accountId, rightfulMachineUsers, Optional.empty());
            umsStateBuilder.addMachineUserToGroupMap(machineUsersToGroupsMap);


            envUmsStateMap.put(envCRN, umsStateBuilder.build());
        });

        return envUmsStateMap;
    }

    private List<User> getUsersWithEnvironmentRights(
        String actorCrn, String envCRN, List<User> allUsers) {
        List<User> rightfulUsers = new ArrayList<>();
        // for all users, check right for the passed envCRN
        for (User u : allUsers) {

            if (umsClient.checkRight(actorCrn, u.getCrn(), environmentWrite, envCRN, Optional.empty())) {
                // if (true) {
                rightfulUsers.add(u);
            }
        }
        return rightfulUsers;

    }

    private List<MachineUser> getMachineUsersWithEnvironmentRights(
        String actorCrn, String envCRN, List<MachineUser> allMachineUsers) {

        List<MachineUser> rightfulMachineUsers = new ArrayList<>();
        // machine users
        for (MachineUser machineUser : allMachineUsers) {

            // Machine User can be a power user also
            if (umsClient.checkRight(actorCrn, machineUser.getCrn(), environmentWrite, envCRN, Optional.empty())) {
                // this is admin user having write access
                rightfulMachineUsers.add(machineUser);
            }
        }

        return rightfulMachineUsers;

    }
}
