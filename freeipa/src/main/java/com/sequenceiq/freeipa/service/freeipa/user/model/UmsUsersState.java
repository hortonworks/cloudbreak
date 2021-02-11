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

    // TODO: CDPCP-3994: It would be cleaner to model the user's CRN as a property of FmsUser. But FmsUser is used to represent users read from the
    //  IPA as well as from the UMS, and we don't always know the CRN of a user read from the IPA. Once the rollout of the workload credentials sync
    //  optimization is complete, and all production environments have been sync'd, we can rely on the CRN being present in the IPA record, and can
    //  then move the CRN into FmsUser.
    private final ImmutableMap<String, String> userToCrnMap;

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    private UmsUsersState(
            UsersState usersState, Map<String, WorkloadCredential> usersWorkloadCredentialMap,
            Collection<String> requestedWorkloadUsernames, Collection<FmsGroup> workloadAdministrationGroups,
            Map<String, List<CloudIdentity>> userToCloudIdentityMap,
            Collection<ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities,
            Map<String, String> userToCrnMap) {
        this.usersState = requireNonNull(usersState, "UsersState is null");
        this.usersWorkloadCredentialMap = ImmutableMap.copyOf(
                requireNonNull(usersWorkloadCredentialMap, "workload credential map is null"));
        this.requestedWorkloadUsernames = ImmutableSet.copyOf(
                requireNonNull(requestedWorkloadUsernames, "requested workload usernames is null"));
        this.workloadAdministrationGroups = ImmutableSet.copyOf(
                requireNonNull(workloadAdministrationGroups, "workloadAdministrationGroups is null"));
        this.userToCloudIdentityMap = ImmutableMap.copyOf(
                requireNonNull(userToCloudIdentityMap, "userToCloudIdentityMap is null"));
        this.servicePrincipalCloudIdentities = ImmutableList.copyOf(
                requireNonNull(servicePrincipalCloudIdentities, "servicePrincipalCloudIdentities is null"));
        this.userToCrnMap = ImmutableMap.copyOf(
                requireNonNull(userToCrnMap, "userToCrnMap is null"));
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

    public ImmutableMap<String, String> getUserToCrnMap() {
        return userToCrnMap;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private UsersState usersState;

        private Map<String, WorkloadCredential> workloadCredentialMap = new HashMap<>();

        private Set<String> requestedWorkloadUsernames = new HashSet<>();

        private Collection<FmsGroup> workloadAdministrationGroups = Set.of();

        private Map<String, List<CloudIdentity>> userToCloudIdentityMap = new HashMap<>();

        private Collection<ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities = new ArrayList<>();

        private Map<String, String> userToCrnMap = new HashMap<>();

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

        public Builder setWorkloadAdministrationGroups(Collection<FmsGroup> workloadAdministrationGroups) {
            this.workloadAdministrationGroups = workloadAdministrationGroups;
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

        public Builder addUserCrn(String userName, String crn) {
            userToCrnMap.put(userName, crn);
            return this;
        }

        public UmsUsersState build() {
            return new UmsUsersState(usersState, workloadCredentialMap, requestedWorkloadUsernames,
                    workloadAdministrationGroups, userToCloudIdentityMap, servicePrincipalCloudIdentities, userToCrnMap);
        }
    }

}
