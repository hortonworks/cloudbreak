package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class UsersState {
    private final ImmutableSet<FmsGroup> groups;

    private final ImmutableSet<FmsUser> users;

    private final ImmutableMultimap<String, String> groupMembership;

    private final ImmutableMap<String, UserMetadata> userMetadataMap;

    public UsersState(
        Set<FmsGroup> groups, Set<FmsUser> users, Multimap<String, String> groupMembership, Map<String, UserMetadata> userMetadataMap) {
        this.groups = ImmutableSet.copyOf(requireNonNull(groups, "groups is null"));
        this.users = ImmutableSet.copyOf(requireNonNull(users, "users is null"));
        this.groupMembership = ImmutableMultimap.copyOf(requireNonNull(groupMembership, "group membership is null"));
        this.userMetadataMap = ImmutableMap.copyOf(requireNonNull(userMetadataMap, "user metadata map is null"));
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

    public ImmutableMap<String, UserMetadata> getUserMetadataMap() {
        return userMetadataMap;
    }

    @Override
    public String toString() {
        return "UsersState{"
                + "groups=" + groups
                + ", users=" + users
                + ", groupMembership=" + groupMembership
                + ", userMetadataMap=" + userMetadataMap
                + '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Set<FmsGroup> fmsGroups = new HashSet<>();

        private Set<FmsUser> fmsUsers = new HashSet<>();

        private Multimap<String, String> groupMembership = HashMultimap.create();

        private Map<String, UserMetadata> userMetadataMap = new HashMap<>();

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

        public Builder addUserMetadata(String userName, UserMetadata userMetadata) {
            userMetadataMap.put(userName, userMetadata);
            return this;
        }

        public UsersState build() {
            return new UsersState(fmsGroups, fmsUsers, groupMembership, userMetadataMap);
        }
    }
}