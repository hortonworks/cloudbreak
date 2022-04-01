package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

public class UsersStateDifference {

    private final ImmutableSet<FmsGroup> groupsToAdd;

    private final ImmutableSet<FmsGroup> groupsToRemove;

    private final ImmutableSet<FmsUser> usersToAdd;

    private final ImmutableSet<String> usersWithCredentialsToUpdate;

    private final ImmutableSet<String> usersToRemove;

    private final ImmutableSet<String> usersToDisable;

    private final ImmutableSet<String> usersToEnable;

    private final ImmutableMultimap<String, String> groupMembershipToAdd;

    private final ImmutableMultimap<String, String> groupMembershipToRemove;

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    public UsersStateDifference(ImmutableSet<FmsGroup> groupsToAdd, ImmutableSet<FmsGroup> groupsToRemove,
            ImmutableSet<FmsUser> usersToAdd, ImmutableSet<String> usersWithCredentialsToUpdate, ImmutableSet<String> usersToRemove,
            ImmutableMultimap<String, String> groupMembershipToAdd, ImmutableMultimap<String, String> groupMembershipToRemove,
            ImmutableSet<String> usersToDisable, ImmutableSet<String> usersToEnable) {
        this.groupsToAdd = requireNonNull(groupsToAdd);
        this.groupsToRemove = requireNonNull(groupsToRemove);
        this.usersToAdd = requireNonNull(usersToAdd);
        this.usersWithCredentialsToUpdate = requireNonNull(usersWithCredentialsToUpdate);
        this.usersToRemove = requireNonNull(usersToRemove);
        this.groupMembershipToAdd = requireNonNull(groupMembershipToAdd);
        this.groupMembershipToRemove = requireNonNull(groupMembershipToRemove);
        this.usersToDisable = requireNonNull(usersToDisable);
        this.usersToEnable = requireNonNull(usersToEnable);
    }

    public ImmutableSet<FmsGroup> getGroupsToAdd() {
        return groupsToAdd;
    }

    public ImmutableSet<FmsGroup> getGroupsToRemove() {
        return groupsToRemove;
    }

    public ImmutableSet<FmsUser> getUsersToAdd() {
        return usersToAdd;
    }

    public ImmutableSet<String> getUsersWithCredentialsToUpdate() {
        return usersWithCredentialsToUpdate;
    }

    public ImmutableSet<String> getUsersToRemove() {
        return usersToRemove;
    }

    public ImmutableMultimap<String, String> getGroupMembershipToAdd() {
        return groupMembershipToAdd;
    }

    public ImmutableMultimap<String, String> getGroupMembershipToRemove() {
        return groupMembershipToRemove;
    }

    public ImmutableSet<String> getUsersToDisable() {
        return usersToDisable;
    }

    public ImmutableSet<String> getUsersToEnable() {
        return usersToEnable;
    }

    @Override
    public String toString() {
        return "UsersStateDifference{"
                + "groupsToAdd=" + groupsToAdd
                + ", groupsToRemove=" + groupsToRemove
                + ", usersToAdd=" + usersToAdd
                + ", usersWithCredentialsToUpdate=" + usersWithCredentialsToUpdate
                + ", usersToRemove=" + usersToRemove
                + ", groupMembershipToAdd=" + groupMembershipToAdd
                + ", groupMembershipToRemove=" + groupMembershipToRemove
                + ", usersToDisable=" + usersToDisable
                + ", usersToEnable=" + usersToEnable
                + '}';
    }
}
