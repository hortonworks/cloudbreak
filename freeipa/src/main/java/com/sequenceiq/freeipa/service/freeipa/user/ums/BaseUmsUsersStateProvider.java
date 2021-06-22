package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsGroupConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> wags,
            String environmentCrn) {
        Set<String> wagNamesForOtherEnvironments = new HashSet<>();
        // Only add workload admin groups that belong to this environment.
        // At the same time, build a set of workload admin groups that are
        // associated with other environments so we can filter these out in
        // the per-user group listing in handleUser.
        wags.entrySet().forEach(wagEntry -> {
            UserManagementProto.WorkloadAdministrationGroup wag = wagEntry.getKey();
            String groupName = wag.getWorkloadAdministrationGroupName();
            if (wag.getResource().equalsIgnoreCase(environmentCrn)) {
                builder.addGroup(wagEntry.getValue());
            } else {
                Crn resourceCrn = getCrn(wag);
                if (resourceCrn != null && resourceCrn.getService() == Crn.Service.ENVIRONMENTS
                        && resourceCrn.getResourceType() == Crn.ResourceType.ENVIRONMENT) {
                    wagNamesForOtherEnvironments.add(groupName);
                }
            }
        });
        return wagNamesForOtherEnvironments;
    }

    private Crn getCrn(UserManagementProto.WorkloadAdministrationGroup wag) {
        Crn resourceCrn = null;
        try {
            resourceCrn = Crn.fromString(wag.getResource());
        } catch (Exception e) {
            LOGGER.debug("Invalid resource is assigned to workload admin group: {}", e.getMessage());
        }
        return resourceCrn;
    }
}
