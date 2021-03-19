package com.sequenceiq.freeipa.service.freeipa.user;

import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class UserSyncRequestFilter {

    private final ImmutableSet<String> userCrnFilter;

    private final ImmutableSet<String> machineUserCrnFilter;

    private final Optional<String> deletedWorkloadUser;

    private final boolean fullSync;

    public UserSyncRequestFilter(Set<String> userCrnFilter, Set<String> machineUserCrnFilter, Optional<String> deletedWorkloadUser) {
        this.userCrnFilter = ImmutableSet.copyOf(userCrnFilter);
        this.machineUserCrnFilter = ImmutableSet.copyOf(machineUserCrnFilter);
        this.deletedWorkloadUser = deletedWorkloadUser;
        fullSync = userCrnFilter.isEmpty() && machineUserCrnFilter.isEmpty();
    }

    public static UserSyncRequestFilter newFullSync() {
        return new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
    }

    public boolean isFullSync() {
        return fullSync;
    }

    public ImmutableSet<String> getUserCrnFilter() {
        return userCrnFilter;
    }

    public ImmutableSet<String> getMachineUserCrnFilter() {
        return machineUserCrnFilter;
    }

    public Optional<String> getDeletedWorkloadUser() {
        return deletedWorkloadUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserSyncRequestFilter that = (UserSyncRequestFilter) o;
        return userCrnFilter.equals(that.userCrnFilter) &&
                machineUserCrnFilter.equals(that.machineUserCrnFilter) &&
                deletedWorkloadUser.equals(that.deletedWorkloadUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userCrnFilter, machineUserCrnFilter, deletedWorkloadUser);
    }
}
