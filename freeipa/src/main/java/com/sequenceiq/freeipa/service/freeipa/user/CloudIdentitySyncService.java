package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentity;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.freeipa.configuration.CloudIdSyncConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.polling.usersync.CloudIdSyncPollerObject;
import com.sequenceiq.freeipa.service.polling.usersync.CloudIdSyncStatusListenerTask;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;

@Service
public class CloudIdentitySyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudIdentitySyncService.class);

    private static final int ONE_MAX_CONSECUTIVE_FAILURE = 1;

    @Inject
    private Clock clock;

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private CloudIdSyncConfig config;

    @Inject
    private PollingService<CloudIdSyncPollerObject> cloudIdSyncPollingService;

    @Inject
    private CloudIdSyncStatusListenerTask cloudIdSyncStatusListenerTask;

    public void syncCloudIdentities(Stack stack, UmsUsersState umsUsersState, BiConsumer<String, String> warnings) {
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

            LOGGER.debug("Setting ranger cloud identity mapping: {}", setRangerCloudIdentityMappingRequest);
            RangerCloudIdentitySyncStatus syncStatus = sdxEndpoint.setRangerCloudIdentityMapping(envCrn, setRangerCloudIdentityMappingRequest);

            // The sync status represents a cloud identity sync that may still be in progress, which we need to poll to check for completion.
            checkSyncStatus(syncStatus, envCrn, warnings);
        } catch (Exception e) {
            LOGGER.warn("Failed to set cloud identity mapping for environment {}", envCrn, e);
            warnings.accept(envCrn, "Failed to set cloud identity mapping");
        }
    }

    private Map<String, String> getAzureUserMapping(UmsUsersState umsUsersState) {
        Map<String, List<CloudIdentity>> userCloudIdentites = umsUsersState.getUserToCloudIdentityMap();
        Map<String, String> userToAzureObjectIdMap = getAzureObjectIdMap(userCloudIdentites);
        Map<String, String> servicePrincipalObjectIdMap = getAzureObjectIdMap(umsUsersState.getServicePrincipalCloudIdentities());

        Map<String, String> azureUserMapping = new HashMap<>();
        azureUserMapping.putAll(userToAzureObjectIdMap);
        azureUserMapping.putAll(servicePrincipalObjectIdMap);
        return azureUserMapping;
    }

    private void checkSyncStatus(RangerCloudIdentitySyncStatus syncStatus, String envCrn, BiConsumer<String, String> warnings) {
        LOGGER.info("syncStatus = {}", syncStatus);
        switch (syncStatus.getState()) {
            case SUCCESS:
                LOGGER.info("Successfully synced cloud identity, envCrn = {}", envCrn);
                return;
            case NOT_APPLICABLE:
                LOGGER.info("Cloud identity sync not applicable, envCrn = {}", envCrn);
                return;
            case FAILED:
                LOGGER.error("Failed to sync cloud identity, envCrn = {}", envCrn);
                warnings.accept(envCrn, "Failed to sync cloud identity into environment");
                return;
            case ACTIVE:
                // NOTE: Although it's synchronously polling, in practice this sync takes less than a second to complete
                LOGGER.info("Sync is still in progress, attempting to poll sync status for envCrn = {}", envCrn);
                pollSyncStatus(envCrn, syncStatus.getCommandId(), warnings);
                break;
            default:
                warnings.accept(envCrn, "Encountered unknown cloud identity sync state");
        }
    }

    private void pollSyncStatus(String environmentCrn, long commandId, BiConsumer<String, String> warnings) {
        CloudIdSyncPollerObject pollerObject = new CloudIdSyncPollerObject(environmentCrn, commandId);
        ExtendedPollingResult result = cloudIdSyncPollingService.pollWithAbsoluteTimeout(cloudIdSyncStatusListenerTask, pollerObject,
                config.getPollerSleepIntervalMs(), config.getPollerTimeoutSeconds(), ONE_MAX_CONSECUTIVE_FAILURE);
        if (!result.isSuccess()) {
            String errMsg = String.format("Failed to poll cloud id sync status, envCrn = %s, polling result = %s", environmentCrn, result.getPollingResult());
            Exception ex = result.getException();
            LOGGER.error(errMsg, ex);
            warnings.accept(environmentCrn, "Failed to sync cloud identity into environment");
        }
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
}
