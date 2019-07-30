package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class UsersState {
    private Set<FmsGroup> groups;

    private Set<FmsUser> users;

    private Multimap<String, String> groupMembership;

    public UsersState(Set<FmsGroup> groups, Set<FmsUser> users, Multimap<String, String> groupMembership) {
        this.groups = requireNonNull(groups);
        this.users = requireNonNull(users);
        this.groupMembership = requireNonNull(groupMembership);
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

        public void addGroup(FmsGroup fmsGroup) {
            fmsGroups.add(fmsGroup);
        }

        public void addUser(FmsUser fmsUser) {
            fmsUsers.add(fmsUser);
        }

        public void addMemberToGroup(String group, String user) {
            groupMembership.put(group, user);
        }

        public UsersState build() {
            return new UsersState(fmsGroups, fmsUsers, groupMembership);
        }
    }
}