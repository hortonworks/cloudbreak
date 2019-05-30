package com.sequenceiq.freeipa.service.user.model;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;

public class UsersStateDifference {
    private Set<Group> groupsToAdd;

    private Set<User> usersToAdd;

    private Set<Group> groupsToRemove;

    private Set<User> usersToRemove;

    private Multimap<Group, User> groupMembershipToAdd;

    private Multimap<Group, User> groupMembershipToRemove;

    public UsersStateDifference(Set<Group> groupsToAdd, Set<User> usersToAdd, Set<Group> groupsToRemove, Set<User> usersToRemove,
            Multimap<Group, User> groupMembershipToAdd, Multimap<Group, User> groupMembershipToRemove) {
        this.groupsToAdd = requireNonNull(groupsToAdd);
        this.usersToAdd = requireNonNull(usersToAdd);
        this.groupsToRemove = requireNonNull(groupsToRemove);
        this.usersToRemove = requireNonNull(usersToRemove);
        this.groupMembershipToAdd = requireNonNull(groupMembershipToAdd);
        this.groupMembershipToRemove = requireNonNull(groupMembershipToRemove);
    }

    public Set<Group> getGroupsToAdd() {
        return groupsToAdd;
    }

    public Set<User> getUsersToAdd() {
        return usersToAdd;
    }

    public Set<Group> getGroupsToRemove() {
        return groupsToRemove;
    }

    public Set<User> getUsersToRemove() {
        return usersToRemove;
    }

    public Multimap<Group, User> getGroupMembershipToAdd() {
        return groupMembershipToAdd;
    }

    public Multimap<Group, User> getGroupMembershipToRemove() {
        return groupMembershipToRemove;
    }

    @Override
    public String toString() {
        return "UsersStateDifference{"
                + "groupsToAdd=" + groupsToAdd
                + ", usersToAdd=" + usersToAdd
                + ", groupsToRemove=" + groupsToRemove
                + ", usersToRemove=" + usersToRemove
                + ", groupMembershipToAdd=" + groupMembershipToAdd
                + ", groupMembershipToRemove=" + groupMembershipToRemove
                + '}';
    }

    public static UsersStateDifference fromUmsAndIpaUsersStates(UsersState umsState, UsersState ipaState) {
        return new UsersStateDifference(
                Set.copyOf(Sets.difference(umsState.getGroups(), ipaState.getGroups())),
                Set.copyOf(Sets.difference(umsState.getUsers(), ipaState.getUsers())),
                Set.copyOf(Sets.difference(ipaState.getGroups(), umsState.getGroups())),
                Set.copyOf(Sets.difference(ipaState.getUsers(), umsState.getUsers())),
                // TODO calculate group membership changes
                Multimaps.forMap(Map.of()),
                Multimaps.forMap(Map.of()));
    }
}
