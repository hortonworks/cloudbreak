package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class UsersState {
    private ImmutableSet<FmsGroup> groups;

    private ImmutableSet<FmsUser> users;

    private ImmutableMultimap<String, String> groupMembership;

    public UsersState(
        Set<FmsGroup> groups, Set<FmsUser> users, Multimap<String, String> groupMembership) {
        this.groups = ImmutableSet.copyOf(requireNonNull(groups, "groups is null"));
        this.users = ImmutableSet.copyOf(requireNonNull(users, "users is null"));
        this.groupMembership = ImmutableMultimap.copyOf(requireNonNull(groupMembership, "group membership is null"));
    }

    public ImmutableSet<FmsGroup> getGroups() {
        return groups;
    }

    public ImmutableSet<FmsUser> getUsers() {
        return users;
    }

    public ImmutableMultimap<String, String> getGroupMembership() {
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

        public Builder addGroup(FmsGroup fmsGroup) {
            fmsGroups.add(fmsGroup);
            return this;
        }

        public Builder addUser(FmsUser fmsUser) {
            fmsUsers.add(fmsUser);
            return this;
        }

        public Builder addMemberToGroup(String group, String user) {
            groupMembership.put(group, user);
            return this;
        }

        public UsersState build() {
            return new UsersState(fmsGroups, fmsUsers, groupMembership);
        }
    }
}