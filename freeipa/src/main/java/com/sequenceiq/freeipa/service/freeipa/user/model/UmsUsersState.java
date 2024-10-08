package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentity;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class UmsUsersState {

    private final UsersState usersState;

    private final ImmutableMap<String, WorkloadCredential> usersWorkloadCredentialMap;

    private final ImmutableSet<String> requestedWorkloadUsernames;

    private final ImmutableSet<FmsGroup> workloadAdministrationGroups;

    private final ImmutableMap<String, List<CloudIdentity>> userToCloudIdentityMap;

    private final ImmutableList<ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities;

    private final ImmutableSet<String> groupsExceedingThreshold;

    private final ImmutableSet<String> groupsExceedingLimit;

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    private UmsUsersState(
            UsersState usersState, Map<String, WorkloadCredential> usersWorkloadCredentialMap,
            Collection<String> requestedWorkloadUsernames, ImmutableSet<FmsGroup> workloadAdministrationGroups,
            Map<String, List<CloudIdentity>> userToCloudIdentityMap,
            Collection<ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities,
            Collection<String> groupsExceedingThreshold,
            Collection<String> groupsExceedingLimit) {
        this.usersState = requireNonNull(usersState, "UsersState is null");
        this.usersWorkloadCredentialMap = ImmutableMap.copyOf(
                requireNonNull(usersWorkloadCredentialMap, "workload credential map is null"));
        this.requestedWorkloadUsernames = ImmutableSet.copyOf(
                requireNonNull(requestedWorkloadUsernames, "requested workload usernames is null"));
        this.workloadAdministrationGroups = requireNonNull(workloadAdministrationGroups, "workloadAdministrationGroups is null");
        this.userToCloudIdentityMap = ImmutableMap.copyOf(
                requireNonNull(userToCloudIdentityMap, "userToCloudIdentityMap is null"));
        this.servicePrincipalCloudIdentities = ImmutableList.copyOf(
                requireNonNull(servicePrincipalCloudIdentities, "servicePrincipalCloudIdentities is null"));
        this.groupsExceedingThreshold = ImmutableSet.copyOf(
                requireNonNull(groupsExceedingThreshold, "groupsExceedingThreshold is null"));
        this.groupsExceedingLimit = ImmutableSet.copyOf(
                requireNonNull(groupsExceedingLimit, "groupsExceedingLimit is null"));
    }

    public UsersState getUsersState() {
        return usersState;
    }

    public ImmutableMap<String, WorkloadCredential> getUsersWorkloadCredentialMap() {
        return usersWorkloadCredentialMap;
    }

    public ImmutableSet<String> getRequestedWorkloadUsernames() {
        return requestedWorkloadUsernames;
    }

    public ImmutableSet<FmsGroup> getWorkloadAdministrationGroups() {
        return workloadAdministrationGroups;
    }

    public ImmutableMap<String, List<CloudIdentity>> getUserToCloudIdentityMap() {
        return userToCloudIdentityMap;
    }

    public ImmutableList<ServicePrincipalCloudIdentities> getServicePrincipalCloudIdentities() {
        return servicePrincipalCloudIdentities;
    }

    public ImmutableSet<String> getGroupsExceedingThreshold() {
        return groupsExceedingThreshold;
    }

    public ImmutableSet<String> getGroupsExceedingLimit() {
        return groupsExceedingLimit;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private UsersState usersState;

        private Map<String, WorkloadCredential> workloadCredentialMap = new HashMap<>();

        private Set<String> requestedWorkloadUsernames = new HashSet<>();

        private ImmutableSet<FmsGroup> workloadAdministrationGroups = ImmutableSet.of();

        private Map<String, List<CloudIdentity>> userToCloudIdentityMap = new HashMap<>();

        private Collection<ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities = new ArrayList<>();

        private Collection<String> groupsExceedingThreshold = Set.of();

        private Collection<String> groupsExceedingLimit = Set.of();

        public Builder setUsersState(UsersState usersState) {
            this.usersState = usersState;
            return this;
        }

        public Builder addWorkloadCredentials(String userName, WorkloadCredential creds) {
            workloadCredentialMap.put(userName, creds);
            return this;
        }

        public Builder addRequestedWorkloadUsername(String username) {
            requestedWorkloadUsernames.add(username);
            return this;
        }

        public Builder addAllRequestedWorkloadUsernames(Collection<String> usernames) {
            requestedWorkloadUsernames.addAll(usernames);
            return this;
        }

        public Builder setWorkloadAdministrationGroups(ImmutableSet<FmsGroup> workloadAdministrationGroups) {
            this.workloadAdministrationGroups = requireNonNull(workloadAdministrationGroups);
            return this;
        }

        public Builder addUserCloudIdentities(String user, List<CloudIdentity> cloudIdentities) {
            userToCloudIdentityMap.put(user, cloudIdentities);
            return this;
        }

        public Builder addServicePrincipalCloudIdentities(Collection<ServicePrincipalCloudIdentities> cloudIdentities) {
            servicePrincipalCloudIdentities.addAll(cloudIdentities);
            return this;
        }

        public Builder setGroupsExceedingThreshold(Collection<String> groupsExceedingThreshold) {
            this.groupsExceedingThreshold = requireNonNull(groupsExceedingThreshold);
            return this;
        }

        public Builder setGroupsExceedingLimit(Collection<String> groupsExceedingLimit) {
            this.groupsExceedingLimit = requireNonNull(groupsExceedingLimit);
            return this;
        }

        public UmsUsersState build() {
            return new UmsUsersState(usersState, workloadCredentialMap, requestedWorkloadUsernames,
                    workloadAdministrationGroups, userToCloudIdentityMap, servicePrincipalCloudIdentities,
                    groupsExceedingThreshold, groupsExceedingLimit);
        }
    }

}
