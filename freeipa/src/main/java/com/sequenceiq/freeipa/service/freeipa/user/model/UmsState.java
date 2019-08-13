package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UmsState {
    // Also contains machine user's crn
    private Map<String, List<Group>> userToGroupsMap;

    // Regular users
    private Map<String, User> userMap;

    // Admin Users for an environment.
    private Map<String, User> adminUserMap;

    private Map<String, MachineUser> adminMachineUserMap = new HashMap<>();

    private Map<String, MachineUser> machineUserMap;

    public UmsState(Map<String, List<Group>> userToGroupsMap, Map<String, User> adminUserMap,
                    Map<String, MachineUser> adminMachineUserMap, Map<String, User> userMap,
                    Map<String, MachineUser> machineUserMap) {

        this.userToGroupsMap = userToGroupsMap;
        this.adminUserMap = requireNonNull(adminUserMap);
        this.adminMachineUserMap = adminMachineUserMap;
        this.userMap = requireNonNull(userMap);
        this.machineUserMap = requireNonNull(machineUserMap);
    }

    public UsersState getUsersState(String environmentCrn) {

        // TODO: Might not be needed if customer provided groups membership is done. Then we sync all members group.
        String adminGrpName = "cpd_env_admin_ap-test-env";

        UsersState.Builder builder = new UsersState.Builder();

        // TODO: Question: Why is this used for.
        Map<String, FmsGroup> crnToGroup = new HashMap<>();

        userToGroupsMap.entrySet()
                .forEach(e -> {
                    // TODO: list of all groups to be added and also membership
                    FmsUser user = umsUserToUser(userMap.get(e.getKey()));
                    e.getValue().forEach(g -> {
                        FmsGroup group = umsGroupToGroup(g);
                        crnToGroup.put(g.getCrn(), group);
                        builder.addMemberToGroup(group.getName(), user.getName());
                        builder.addGroup(group);
                    });
                });

        // Regular Users
        userMap.entrySet()
                .forEach(e -> {
                    FmsUser fmsUser = umsUserToUser(e.getValue());
                    builder.addUser(fmsUser);
                });


        // TODO: might not be needed env Admin Users
        adminUserMap.entrySet()
            .forEach(adminUser -> {
                // Admin users are also regular users but must be added to specific group. Admin Users are already added as regular users
                FmsUser user = umsUserToUser(adminUser.getValue());
                // TODO remove `admins` membership once the group mapping is figured out (CB-2003, DISTX-95)
                // TODO: change admins group to cpd_env_admin_<#env_name>, need to parse env crn and pass that group value.
                builder.addMemberToGroup(adminGrpName, user.getName());
            });

        machineUserMap.entrySet()
                .forEach(e -> {
                    FmsUser fmsUser = umsMachineUserToUser(e.getValue());
                    builder.addUser(fmsUser);
                });

        // TODO: Might not be needed, since all the groups are coming from ums. Machine Admin Users
        adminMachineUserMap.entrySet()
                .forEach(e -> {
                    FmsUser user = umsMachineUserToUser(e.getValue());
                    // TODO remove `admins` membership once the group mapping is figured out (CB-2003, DISTX-95)
                    // TODO: change admins group to cpd_env_admin_<#env_name>, need to parse env crn and pass that group value.
                    builder.addMemberToGroup(adminGrpName, user.getName());
                });

        return builder.build();
    }

    public Set<String> getUsernamesFromCrns(Collection<String> userCrns, Collection<String> machineUserCrns) {
        return Stream.concat(userCrns.stream().map(crn -> getWorkloadUsername(userMap.get(crn))),
                machineUserCrns.stream().map(crn -> getWorkloadUsername(machineUserMap.get(crn))))
                .collect(Collectors.toSet());
    }

    private FmsUser umsUserToUser(User umsUser) {
        FmsUser fmsUser = new FmsUser();
        fmsUser.setName(getWorkloadUsername(umsUser));
        fmsUser.setFirstName(getOrDefault(umsUser.getFirstName(), "None"));
        fmsUser.setLastName(getOrDefault(umsUser.getLastName(), "None"));
        return fmsUser;
    }

    private String getOrDefault(String value, String other) {
        return (value == null || value.isBlank()) ? other : value;
    }

    private String getWorkloadUsername(User umsUser) {
        return umsUser.getWorkloadUsername();
    }

    private String getWorkloadUsername(MachineUser umsMachineUser) {
        return umsMachineUser.getWorkloadUsername();
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

    @Override
    public String toString() {
        return "UmsState{"
                + "userToGroupsMap=" + userToGroupsMap
                + ", userMap=" + userMap
                + ", adminUserMap=" + adminUserMap
                + ", adminMachineUserMap=" + adminMachineUserMap
                + ", machineUserMap=" + machineUserMap
                + '}';
    }

    public static class Builder {
        private Map<String, Group> groupMap = new HashMap<>();

        private Map<String, List<Group>> userToGroupsMap = new HashMap<>();

        private Map<String, User> userMap = new HashMap<>();

        private Map<String, User> adminUserMap = new HashMap<>();

        private Map<String, MachineUser> adminMachineUserMap = new HashMap<>();

        private Map<String, MachineUser> machineUserMap = new HashMap<>();

        public void addUserToGroupMap(Map<String, List<Group>> userToGroupsMap) {
            this.userToGroupsMap = userToGroupsMap;
        }

        public void addGroup(Group group) {
            groupMap.put(group.getCrn(), group);
        }

        public void addUsers(List<User> users) {
            for (User u : users) {
                addUser(u);
            }
        }

        public void addUser(User user) {
            String userCrn = user.getCrn();
            userMap.put(userCrn, user);
        }

        public void addAdminUser(User user) {
            String userCrn = user.getCrn();
            adminUserMap.put(userCrn, user);
        }

        public void addAdminMachineUser(MachineUser machineAdminuser) {
            // Machine User can be a power user also.
            String userCrn = machineAdminuser.getCrn();
            adminMachineUserMap.put(userCrn, machineAdminuser);
        }

        public void addMachineUsers(List<MachineUser> users) {
            for (MachineUser u : users) {
                addMachineUser(u);
            }
        }

        public void addMachineUser(MachineUser machineUser) {
            String machineUserCrn = machineUser.getCrn();
            machineUserMap.put(machineUserCrn, machineUser);
        }

        public UmsState build() {
            return new UmsState(userToGroupsMap, adminUserMap, adminMachineUserMap, userMap, machineUserMap);
        }
    }
}
