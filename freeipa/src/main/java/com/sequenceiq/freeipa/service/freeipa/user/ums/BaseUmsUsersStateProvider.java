package com.sequenceiq.freeipa.service.freeipa.user.ums;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsGroupConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
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

    void addGroupsToUsersStateBuilder(UsersState.Builder builder, Collection<FmsGroup> groups) {
        groups.forEach(builder::addGroup);
        // Add internal usersync group for each environment
        builder.addGroup(fmsGroupConverter.nameToGroup(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP));
    }

    List<UserManagementProto.WorkloadAdministrationGroup> addWagsToUsersStateBuilder(
            UsersState.Builder builder,
            Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> wags,
            String environmentCrn) {
        List<UserManagementProto.WorkloadAdministrationGroup> relatedWags = Lists.newArrayList();
        wags.entrySet().forEach(wagEntry -> {
            UserManagementProto.WorkloadAdministrationGroup wag = wagEntry.getKey();
            if (wag.getResource().equalsIgnoreCase(environmentCrn)) {
                builder.addGroup(wagEntry.getValue());
                relatedWags.add(wag);
            }
        });
        return relatedWags;
    }
}
