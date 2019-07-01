package com.sequenceiq.freeipa.service.user.model;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

public class UmsState {
    private Map<String, Group> groupMap;

    private Map<String, User> userMap;

    private Map<String, GetRightsResponse> userRightsMap;

    private Map<String, MachineUser> machineUserMap;

    private Map<String, GetRightsResponse> machineUserRightsMap;

    public UmsState(Map<String, Group> groupMap, Map<String, User> userMap, Map<String, GetRightsResponse> userRightsMap,
            Map<String, MachineUser> machineUserMap, Map<String, GetRightsResponse> machineUserRightsMap) {
        this.groupMap = requireNonNull(groupMap);
        this.userMap = requireNonNull(userMap);
        this.userRightsMap = requireNonNull(userRightsMap);
        this.machineUserMap = requireNonNull(machineUserMap);
        this.machineUserRightsMap = requireNonNull(machineUserRightsMap);
    }

    public UsersState getUsersState(String environmentCrn) {
        UsersState.Builder builder = new UsersState.Builder();

        Map<String, com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group> crnToGroup = new HashMap<>(groupMap.size());
        groupMap.entrySet()
                .forEach(e -> {
                    com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group group = umsGroupToGroup(e.getValue());
                    crnToGroup.put(e.getKey(), group);
                    builder.addGroup(group);
                });

        // TODO filter users by environment rights
        userMap.entrySet()
                .forEach(e -> {
                    com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = umsUserToUser(e.getValue());
                    builder.addUser(user);
                    // TODO remove `admins` membership once the group mapping is figured out (CB-2003, DISTX-95)
                    builder.addMemberToGroup("admins", user.getName());
                    userRightsMap.get(e.getKey()).getGroupCrnList()
                            .forEach(crn -> builder.addMemberToGroup(crnToGroup.get(crn).getName(), user.getName()));
                });

        // TODO filter machine users by environment rights
        machineUserMap.entrySet()
                .forEach(e -> {
                    com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = umsMachineUserToUser(e.getValue());
                    builder.addUser(user);
                    machineUserRightsMap.get(e.getKey()).getGroupCrnList()
                            .forEach(crn -> builder.addMemberToGroup(crnToGroup.get(crn).getName(), user.getName()));
                });

        return builder.build();
    }

    public Set<String> getUsernamesFromCrns(Set<String> userCrns) {
        return userCrns.stream()
                .map(crn -> getUsernameFromEmail(userMap.get(crn)))
                .collect(Collectors.toSet());
    }

    private com.sequenceiq.freeipa.api.v1.freeipa.user.model.User umsUserToUser(User umsUser) {
        com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = new com.sequenceiq.freeipa.api.v1.freeipa.user.model.User();
        // TODO Use workloadUsername once the UMS proto is updated in DISTX-184
        user.setName(getUsernameFromEmail(umsUser));
        user.setFirstName(getOrDefault(umsUser.getFirstName(), "None"));
        user.setLastName(getOrDefault(umsUser.getLastName(), "None"));
        return user;
    }

    private String getOrDefault(String value, String other) {
        return (value == null || value.isBlank()) ? other : value;
    }

    private String getUsernameFromEmail(User umsUser) {
        // TODO replace this code with workloadUsername in DISTX-184
        return umsUser.getEmail().split("@")[0];
    }

    private com.sequenceiq.freeipa.api.v1.freeipa.user.model.User umsMachineUserToUser(MachineUser umsMachineUser) {
        com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = new com.sequenceiq.freeipa.api.v1.freeipa.user.model.User();
        // TODO Use workloadUsername once the UMS proto is updated in DISTX-184
        user.setName(umsMachineUser.getMachineUserName());
        // TODO what should the appropriate first and last name be for machine users?
        user.setFirstName("Machine");
        user.setLastName("User");
        return user;
    }

    private com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group umsGroupToGroup(Group umsGroup) {
        com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group group = new com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group();
        group.setName(umsGroup.getGroupName());
        return group;
    }

    public static class Builder {
        private Map<String, Group> groupMap = new HashMap<>();

        private Map<String, User> userMap = new HashMap<>();

        private Map<String, GetRightsResponse> userRightsMap = new HashMap<>();

        private Map<String, MachineUser> machineUserMap = new HashMap<>();

        private Map<String, GetRightsResponse> machineUserRightsMap = new HashMap<>();

        public void addGroup(Group group) {
            groupMap.put(group.getCrn(), group);
        }

        public void addUser(User user, GetRightsResponse rights) {
            String userCrn = user.getCrn();
            userMap.put(userCrn, user);
            userRightsMap.put(userCrn, rights);
        }

        public void addMachineUser(MachineUser machineUser, GetRightsResponse rights) {
            String machineUserCrn = machineUser.getCrn();
            machineUserMap.put(machineUserCrn, machineUser);
            machineUserRightsMap.put(machineUserCrn, rights);
        }

        public UmsState build() {
            return new UmsState(groupMap, userMap, userRightsMap, machineUserMap, machineUserRightsMap);
        }
    }
}
