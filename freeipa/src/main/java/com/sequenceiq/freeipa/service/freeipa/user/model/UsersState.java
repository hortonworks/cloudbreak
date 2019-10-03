package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class UsersState {
    private Set<FmsGroup> groups;

    private Set<FmsUser> users;

    private Multimap<String, String> groupMembership;

    private Map<String, WorkloadCredential> usersWorkloadCredentialMap;

    // This field denotes all users those are passwd as an input. If there is no user passwd then this will represent all users.
    private Set<FmsUser> requestedWorkloadUsers = new HashSet<>();

    public UsersState(
        Set<FmsGroup> groups, Set<FmsUser> users, Multimap<String, String> groupMembership,
        Map<String, WorkloadCredential> usersWorkloadCredentialMap, Set<FmsUser> requestedWorkloadUsers) {
        this.groups = requireNonNull(groups);
        this.users = requireNonNull(users);
        this.groupMembership = requireNonNull(groupMembership);
        this.usersWorkloadCredentialMap = usersWorkloadCredentialMap;
        this.requestedWorkloadUsers = requestedWorkloadUsers;
    }

    public Set<FmsGroup> getGroups() {
        return groups;
    }

    public Set<FmsUser> getUsers() {
        return users;
    }

    public Multimap<String, String> getGroupMembership() {
        return groupMembership;
    }

    public Map<String, WorkloadCredential> getUsersWorkloadCredentialMap() {
        return usersWorkloadCredentialMap;
    }

    public Set<FmsUser> getRequestedWorkloadUsers() {
        return requestedWorkloadUsers;
    }

    @Override
    public String toString() {
        return "UsersState{"
                + "groups=" + groups
                + ", users=" + users
                + ", groupMembership=" + groupMembership
                + '}';
    }

    public static class Builder {
        private Set<FmsGroup> fmsGroups = new HashSet<>();

        private Set<FmsUser> fmsUsers = new HashSet<>();

        private Multimap<String, String> groupMembership = HashMultimap.create();

        private Map<String, WorkloadCredential> workloadCredentialMap = new HashMap<>();

        // This field denotes all users those are passwd as an input. If there is no user passwd then this will represent all users.
        private Set<FmsUser> requestedWorkloadUsers = new HashSet<>();

        public void addGroup(FmsGroup fmsGroup) {
            fmsGroups.add(fmsGroup);
        }

        public void addUser(FmsUser fmsUser) {
            fmsUsers.add(fmsUser);
        }

        public void addMemberToGroup(String group, String user) {
            groupMembership.put(group, user);
        }

        public void addWorkloadCredentials(String userName, WorkloadCredential creds) {
            workloadCredentialMap.put(userName, creds);
        }

        public UsersState build() {
            return new UsersState(fmsGroups, fmsUsers, groupMembership, workloadCredentialMap, requestedWorkloadUsers);
        }

        public void addWorkloadUsername(FmsUser user) {
            requestedWorkloadUsers.add(user);

        }
    }
}