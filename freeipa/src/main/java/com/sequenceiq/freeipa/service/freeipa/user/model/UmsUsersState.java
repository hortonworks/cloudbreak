package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class UmsUsersState {

    private final UsersState usersState;

    private final ImmutableMap<String, WorkloadCredential> usersWorkloadCredentialMap;

    private final ImmutableSet<FmsUser> requestedWorkloadUsers;

    private final ImmutableSet<FmsGroup> workloadAdministrationGroups;

    private UmsUsersState(UsersState usersState, Map<String, WorkloadCredential> usersWorkloadCredentialMap, Set<FmsUser> requestedWorkloadUsers,
            Collection<FmsGroup> workloadAdministrationGroups) {
        this.usersState = requireNonNull(usersState, "UsersState is null");
        this.usersWorkloadCredentialMap = ImmutableMap.copyOf(requireNonNull(usersWorkloadCredentialMap, "workload credential map is null"));
        this.requestedWorkloadUsers = ImmutableSet.copyOf(requireNonNull(requestedWorkloadUsers, "requested workload users is null"));
        this.workloadAdministrationGroups = ImmutableSet.copyOf(requireNonNull(workloadAdministrationGroups, "workloadAdministrationGroups is null"));
    }

    public UsersState getUsersState() {
        return usersState;
    }

    public ImmutableMap<String, WorkloadCredential> getUsersWorkloadCredentialMap() {
        return usersWorkloadCredentialMap;
    }

    public ImmutableSet<FmsUser> getRequestedWorkloadUsers() {
        return requestedWorkloadUsers;
    }

    public ImmutableSet<FmsGroup> getWorkloadAdministrationGroups() {
        return workloadAdministrationGroups;
    }

    public static class Builder {
        private UsersState usersState;

        private Map<String, WorkloadCredential> workloadCredentialMap = new HashMap<>();

        private Set<FmsUser> requestedWorkloadUsers = new HashSet<>();

        private Collection<FmsGroup> workloadAdministrationGroups = Set.of();

        public Builder setUsersState(UsersState usersState) {
            this.usersState = usersState;
            return this;
        }

        public Builder addWorkloadCredentials(String userName, WorkloadCredential creds) {
            workloadCredentialMap.put(userName, creds);
            return this;
        }

        public Builder addRequestedWorkloadUsers(FmsUser user) {
            requestedWorkloadUsers.add(user);
            return this;
        }

        public Builder setWorkloadAdministrationGroups(Collection<FmsGroup> workloadAdministrationGroups) {
            this.workloadAdministrationGroups = workloadAdministrationGroups;
            return this;
        }

        public UmsUsersState build() {
            return new UmsUsersState(usersState, workloadCredentialMap, requestedWorkloadUsers, workloadAdministrationGroups);
        }
    }
}
