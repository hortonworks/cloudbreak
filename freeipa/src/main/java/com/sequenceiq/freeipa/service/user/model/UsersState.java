package com.sequenceiq.freeipa.service.user.model;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;

public class UsersState {
    private Set<Group> groups;

    private Set<User> users;

    private Multimap<String, String> groupMembership;

    public UsersState(Set<Group> groups, Set<User> users, Multimap<String, String> groupMembership) {
        this.groups = requireNonNull(groups);
        this.users = requireNonNull(users);
        this.groupMembership = requireNonNull(groupMembership);
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public Set<User> getUsers() {
        return users;
    }

    public Multimap<String, String> getGroupMembership() {
        return groupMembership;
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
        private Set<Group> groups = new HashSet<>();

        private Set<User> users = new HashSet<>();

        private Multimap<String, String> groupMembership = HashMultimap.create();

        public void addGroup(Group group) {
            groups.add(group);
        }

        public void addUser(User user) {
            users.add(user);
        }

        public void addMemberToGroup(String group, String user) {
            groupMembership.put(group, user);
        }

        public UsersState build() {
            return new UsersState(groups, users, groupMembership);
        }
    }
}