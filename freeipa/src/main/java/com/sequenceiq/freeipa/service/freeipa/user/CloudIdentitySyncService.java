package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentity;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
public class CloudIdentitySyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudIdentitySyncService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    public void syncCloudIdentites(Stack stack, UmsUsersState umsUsersState, BiConsumer<String, String> warnings) {
        LOGGER.info("Syncing cloud identities for stack = {}", stack);
        if (CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform())) {
            LOGGER.info("Syncing Azure Object IDs for stack = {}", stack);
            syncAzureObjectIds(stack, umsUsersState, warnings);
        }
    }

    private void syncAzureObjectIds(Stack stack, UmsUsersState umsUsersState, BiConsumer<String, String> warnings) {
        String envCrn = stack.getEnvironmentCrn();
        LOGGER.info("Syncing Azure Object IDs for environment {}", envCrn);

        try {
            Map<String, String> azureUserMapping = getAzureUserMapping(umsUsersState);
            SetRangerCloudIdentityMappingRequest setRangerCloudIdentityMappingRequest = new SetRangerCloudIdentityMappingRequest();
            setRangerCloudIdentityMappingRequest.setAzureUserMapping(azureUserMapping);
            // TODO The SDX endpoint currently sets the config and triggers refresh. The SDX endpoint should also be updated
            //      to allow polling the status of the refresh.
            LOGGER.debug("Setting ranger cloud identity mapping: {}", setRangerCloudIdentityMappingRequest);
            sdxEndpoint.setRangerCloudIdentityMapping(envCrn, setRangerCloudIdentityMappingRequest);
        } catch (Exception e) {
            LOGGER.warn("Failed to set cloud identity mapping for environment {}", envCrn, e);
            warnings.accept(envCrn, "Failed to set cloud identity mapping");
        }
    }

    private Map<String, String> getAzureUserMapping(UmsUsersState umsUsersState) {
        Map<String, List<CloudIdentity>> userCloudIdentites = getUserCloudIdentitiesToSync(umsUsersState);
        Map<String, String> userToAzureObjectIdMap = getAzureObjectIdMap(userCloudIdentites);
        Map<String, String> servicePrincipalObjectIdMap = getAzureObjectIdMap(umsUsersState.getServicePrincipalCloudIdentities());

        Map<String, String> azureUserMapping = new HashMap<>();
        azureUserMapping.putAll(userToAzureObjectIdMap);
        azureUserMapping.putAll(servicePrincipalObjectIdMap);
        return azureUserMapping;
    }

    private Map<String, String> getAzureObjectIdMap(Map<String, List<CloudIdentity>> cloudIdentityMapping) {
        LOGGER.debug("Exracting Azure Object ID mapping from {}", cloudIdentityMapping);
        ImmutableMap.Builder<String, String> azureObjectIdMap = ImmutableMap.builder();
        cloudIdentityMapping.forEach((key, cloudIdentities) -> {
            Optional<String> azureObjectId = getOptionalAzureObjectId(cloudIdentities);
            if (azureObjectId.isPresent()) {
                azureObjectIdMap.put(key, azureObjectId.get());
            }
        });
        return azureObjectIdMap.build();
    }

    private Map<String, String> getAzureObjectIdMap(List<ServicePrincipalCloudIdentities> servicePrincipalCloudIds) {
        LOGGER.debug("Extracting service principal Azure Object ID mapping from {}", servicePrincipalCloudIds);
        ImmutableMap.Builder<String, String> azureObjectIdMap = ImmutableMap.builder();
        servicePrincipalCloudIds.forEach(spCloudId -> {
            Optional<String> azureObjectId = getOptionalAzureObjectId(spCloudId.getCloudIdentitiesList());
            if (azureObjectId.isPresent()) {
                azureObjectIdMap.put(spCloudId.getServicePrincipal(), azureObjectId.get());
            }
        });
        return azureObjectIdMap.build();
    }

    private Optional<String> getOptionalAzureObjectId(List<CloudIdentity> cloudIdentities) {
        List<CloudIdentity> azureCloudIdentities = cloudIdentities.stream()
                .filter(cloudIdentity -> cloudIdentity.getCloudIdentityName().hasAzureCloudIdentityName())
                .collect(Collectors.toList());
        if (azureCloudIdentities.isEmpty()) {
            return Optional.empty();
        } else if (azureCloudIdentities.size() > 1) {
            throw new IllegalStateException(String.format("List contains multiple azure cloud identities = %s", cloudIdentities));
        } else {
            String azureObjectId = Iterables.getOnlyElement(azureCloudIdentities).getCloudIdentityName().getAzureCloudIdentityName().getObjectId();
            return Optional.of(azureObjectId);
        }
    }

    private Map<String, List<CloudIdentity>> getUserCloudIdentitiesToSync(UmsUsersState umsUsersState) {
        Map<String, List<CloudIdentity>> allUserCloudIdentites = umsUsersState.getUserToCloudIdentityMap();
        Set<String> userFilter = usersWithEnvironmentAccess(umsUsersState);
        return allUserCloudIdentites.entrySet().stream()
                .filter(entry -> userFilter.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> usersWithEnvironmentAccess(UmsUsersState umsUsersState) {
        return umsUsersState.getUsersState().getUsers().stream()
                .map(FmsUser::getName)
                .collect(Collectors.toSet());
    }

}
