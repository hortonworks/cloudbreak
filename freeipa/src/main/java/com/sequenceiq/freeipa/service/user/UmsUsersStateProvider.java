package com.sequenceiq.freeipa.service.user;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;
import com.sequenceiq.freeipa.service.user.model.UsersState;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class UmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProvider.class);

    @Inject
    private ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

    @Inject
    private CrnService crnService;

    @Inject
    private GrpcUmsClient umsClient;

    public User getUser(String userCrn, String environmentCrn) {
        final String actorCrn = threadBaseUserCrnProvider.getUserCrn();

        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User user =
                umsClient.getUserDetails(actorCrn, userCrn, Optional.empty());
        return umsUserToUser(actorCrn, user, environmentCrn);
    }

    public UsersState getUsersState(String environmentCrn) {
        final String actorCrn = threadBaseUserCrnProvider.getUserCrn();
        final String accountId = crnService.getCurrentAccountId();

        List<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User> users =
                umsClient.listUsers(actorCrn, accountId, Optional.empty());
        LOGGER.info("Found {} users", users.size());
        List<User> convertedUsers = users.stream()
                .map(user -> umsUserToUser(actorCrn, user, environmentCrn))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser> machineUsers =
                umsClient.listMachineUsers(actorCrn, accountId, Optional.empty());
        LOGGER.info("Found {} machine users", machineUsers.size());
        List<User> convertedMachineUsers = machineUsers.stream()
                .map(machineUser -> umsMachineUserToUser(actorCrn, machineUser, environmentCrn))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<User> allUsers = new HashSet<>();
        allUsers.addAll(convertedUsers);
        allUsers.addAll(convertedMachineUsers);

        Set<Group> groups = allUsers.stream()
                .map(User::getGroups)
                .flatMap(Set::stream)
                .map(this::fromGroupName)
                .collect(Collectors.toSet());

        return new UsersState(groups, allUsers);
    }

    private User umsUserToUser(String actorCrn, com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User umsUser, String environmentCrn) {
        Set<String> groups = getGroupsForUser(actorCrn, umsUser.getCrn(), environmentCrn);
        if (groups.isEmpty()) {
            return null;
        }

        User user = new User();
        // TODO Use workloadUsername once the UMS proto is updated
        user.setName(getUsernameFromEmail(umsUser));
        // TODO improve handling if names are missing
        user.setFirstName(getOrDefault(umsUser.getFirstName(), "None"));
        user.setLastName(getOrDefault(umsUser.getLastName(), "None"));
        user.setGroups(groups);
        return user;
    }

    private String getOrDefault(String value, String other) {
        return (value == null || value.isBlank()) ? other : value;
    }

    private String getUsernameFromEmail(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User umsUser) {
        // TODO replace this code with workloadUsername in DISTX-184
        return umsUser.getEmail().split("@")[0];
    }

    private User umsMachineUserToUser(String actorCrn,
            com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser umsMachineUser, String environmentCrn) {
        Set<String> groups = getGroupsForUser(actorCrn, umsMachineUser.getCrn(), environmentCrn);
        if (groups.isEmpty()) {
            return null;
        }

        User user = new User();
        // TODO Use workloadUsername once the UMS proto is updated
        user.setName(umsMachineUser.getMachineUserName());
        // TODO what should the appropriate first and last name be for machine users?
        user.setFirstName("Machine");
        user.setLastName("User");
        user.setGroups(groups);
        return user;
    }

    private Set<String> getGroupsForUser(String actorCrn, String userCrn, String environmentCrn) {
        // TODO do we care about '*' rights?
        return new HashSet<>(
                umsClient.getGroupsForUser(actorCrn, userCrn, environmentCrn, Optional.empty())
                        .stream()
                        .map(this::getGroupNameFromGroupCrn)
                        .collect(Collectors.toSet()));
    }

    private String getGroupNameFromGroupCrn(String groupCrn) {
        Crn crn = Crn.safeFromString(groupCrn);
        return crn.getResource().split("/")[0];
    }

    private Group fromGroupName(String groupName) {
        Group group = new Group();
        group.setName(groupName);
        return group;
    }
}
