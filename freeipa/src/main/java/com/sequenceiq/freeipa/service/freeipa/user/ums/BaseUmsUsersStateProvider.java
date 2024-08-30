package com.sequenceiq.freeipa.service.freeipa.user.ums;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadAdministrationGroup;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsGroupConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

public class BaseUmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseUmsUsersStateProvider.class);

    @Inject
    private FmsGroupConverter fmsGroupConverter;

    public FmsGroupConverter getFmsGroupConverter() {
        return fmsGroupConverter;
    }

    Map<String, FmsGroup> convertGroupsToFmsGroups(List<UserManagementProto.Group> groups) {
        return groups.stream().collect(Collectors.toMap(UserManagementProto.Group::getCrn, fmsGroupConverter::umsGroupToGroup));
    }

    Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> convertWagsToFmsGroups(List<UserManagementProto.WorkloadAdministrationGroup> wags) {
        return wags.stream()
                .collect(Collectors.toMap(wag -> wag, wag -> fmsGroupConverter.nameToGroup(wag.getWorkloadAdministrationGroupName())));
    }

    void addServicePrincipalsCloudIdentities(
            UmsUsersState.Builder builder,
            List<UserManagementProto.ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities) {
        builder.addServicePrincipalCloudIdentities(servicePrincipalCloudIdentities);
    }

    void addRequestedWorkloadUsernames(
            UmsUsersState.Builder umsUsersStateBuilder, List<String> requestedWorkloadUsernames) {
        umsUsersStateBuilder.addAllRequestedWorkloadUsernames(requestedWorkloadUsernames);
    }

    void addGroupsToUsersStateBuilder(UsersState.Builder builder, Collection<FmsGroup> groups) {
        groups.forEach(builder::addGroup);
        // Add internal usersync group for each environment
        builder.addGroup(fmsGroupConverter.nameToGroup(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP));
    }

    Set<String> addWagsToUsersStateBuilder(
            UsersState.Builder builder,
            Map<WorkloadAdministrationGroup, FmsGroup> environmentWags,
            String environmentCrn) {
        Set<String> wagNamesForOtherEnvironments = new HashSet<>();
        // Only add workload admin groups that belong to this environment.
        // At the same time, build a set of workload admin groups that are
        // associated with other environments so we can filter these out in
        // the per-user group listing in handleUser.

        environmentWags.forEach((wag, value) -> {
            if (wag.getResource().equalsIgnoreCase(environmentCrn)) {
                builder.addGroup(value);
            } else {
                wagNamesForOtherEnvironments.add(wag.getWorkloadAdministrationGroupName());
            }
        });
        return wagNamesForOtherEnvironments;
    }

    public Map<WorkloadAdministrationGroup, FmsGroup> filterEnvironmentWags(Map<WorkloadAdministrationGroup, FmsGroup> wags) {
        List<WorkloadAdministrationGroup> environmentWagEntries = new LinkedList<>();
        return wags.entrySet().stream().filter(entry -> entry.getKey().getResource().contains("environments")).filter(entry -> {
            Crn resourceCrn = getCrn(entry.getKey());
            return resourceCrn != null && resourceCrn.getService() == Crn.Service.ENVIRONMENTS
                    && resourceCrn.getResourceType() == Crn.ResourceType.ENVIRONMENT;
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Crn getCrn(WorkloadAdministrationGroup wag) {
        Crn resourceCrn = null;
        try {
            resourceCrn = Crn.fromString(wag.getResource());
        } catch (Exception e) {
            LOGGER.debug("Invalid resource is assigned to workload admin group: {}", e.getMessage());
        }
        return resourceCrn;
    }

    protected void setLargeGroups(UmsUsersState.Builder umsUsersStateBuilder, UsersState usersState, UserSyncOptions options) {
        umsUsersStateBuilder.setGroupsExceedingThreshold(getLargeGroups(usersState, options.getLargeGroupThreshold()));
        umsUsersStateBuilder.setGroupsExceedingLimit(getLargeGroups(usersState, options.getLargeGroupLimit()));
    }

    private Set<String> getLargeGroups(UsersState usersState, int sizeThreshold) {
        return usersState.getGroupMembership().asMap().entrySet().stream()
                .filter(entry -> entry.getValue().size() > sizeThreshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
