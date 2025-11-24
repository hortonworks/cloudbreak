package com.sequenceiq.cloudbreak.job.provider;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.ATTRIBUTES;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.ProviderSyncState;

@Service
public class ProviderSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncService.class);

    @Inject
    private ProviderSyncConfig providerSyncConfig;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private CloudContextProvider cloudContextProvider;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private List<ProviderResourceSyncer> providerResourceSyncers;

    public void syncResources(StackDto stack) {
        try {
            CloudContext cloudContext = cloudContextProvider.getCloudContext(stack);
            CloudCredential cloudCredential = credentialClientService.getCloudCredential(stack.getEnvironmentCrn());

            List<CloudResource> cloudResources = getCloudResources(stack);
            LOGGER.debug("Syncing resources for stack: {}", cloudResources.stream()
                    .map(CloudResource::getDetailedInfo)
                    .toList());

            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            List<CloudResourceStatus> resourceStatusList = connector.resources().checkForSyncer(ac, cloudResources);
            List<CloudResource> syncedCloudResources = resourceStatusList.stream()
                    .map(CloudResourceStatus::getCloudResource)
                    .toList();
            LOGGER.debug("Resource sync result for stack: {}", syncedCloudResources.stream()
                    .map(CloudResource::getDetailedInfo)
                    .toList());
            syncedCloudResources = syncedCloudResources.stream()
                    .filter(resource -> shouldPersistForResourceType(resource.getType()))
                    .toList();
            LOGGER.debug("Resources persisted for stack {}", syncedCloudResources.stream()
                    .map(CloudResource::getDetailedInfo)
                    .toList());
            resourceNotifier.notifyUpdates(syncedCloudResources, cloudContext);
            setProviderSyncStatus(stack, syncedCloudResources, cloudResources);
        } catch (Exception e) {
            LOGGER.error("Error during provider sync, skipping and logging it: ", e);
        }
    }

    public boolean shouldPersistForResourceType(ResourceType resourceType) {
        return providerResourceSyncers.stream()
                .filter(syncer -> syncer.getResourceType() == resourceType)
                .findFirst()
                .map(ProviderResourceSyncer::shouldPersist)
                .orElse(true);
    }

    private void setProviderSyncStatus(StackDto stack, List<CloudResource> syncedCloudResources, List<CloudResource> cloudResources) {
        Optional<SkuAttributes> hasBasicSku = hasBasicSku(syncedCloudResources);
        if (hasBasicSku.isPresent()) {
            LOGGER.info("Basic SKU migration is needed for {}, updating status", hasBasicSku.get());
            stackUpdater.addProviderState(
                    stack.getResourceCrn(),
                    stack.getId(),
                    ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED
            );
        } else if (shouldUpgradeOutbound(syncedCloudResources).isPresent() ||
                shouldUpgradeOutbound(filterSyncedResources(cloudResources, syncedCloudResources)).isPresent()) {
            LOGGER.info("Outbound upgrade is needed for {}, updating status",
                    shouldUpgradeOutbound(syncedCloudResources).or(() -> shouldUpgradeOutbound(filterSyncedResources(cloudResources, syncedCloudResources))));
            stackUpdater.addProviderState(
                    stack.getResourceCrn(),
                    stack.getId(),
                    ProviderSyncState.OUTBOUND_UPGRADE_NEEDED
            );
        } else {
            LOGGER.debug("Provider sync have not detected errors for {}, cleaning up error states",
                    syncedCloudResources.stream().map(CloudResource::getDetailedInfo).toList());
            stackUpdater.removeProviderStates(
                    stack.getResourceCrn(),
                    stack.getId(),
                    Set.of(ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED, ProviderSyncState.OUTBOUND_UPGRADE_NEEDED)
            );
        }
    }

    private Optional<SkuAttributes> hasBasicSku(List<CloudResource> syncedCloudResources) {
        return syncedCloudResources.stream()
                .map(cloudResource -> {
                    try {
                        return cloudResource.getParameter(ATTRIBUTES, SkuAttributes.class);
                    } catch (CloudbreakServiceException e) {
                        return new SkuAttributes();
                    }
                }).filter(skuAttributes -> skuAttributes != null
                        && LoadBalancerSku.BASIC.getTemplateName().equalsIgnoreCase(skuAttributes.getSku()))
                .findFirst();
    }

    private Optional<NetworkAttributes> shouldUpgradeOutbound(List<CloudResource> syncedCloudResources) {
        return syncedCloudResources.stream()
                .map(cloudResource -> {
                    try {
                        return cloudResource.getParameter(ATTRIBUTES, NetworkAttributes.class);
                    } catch (CloudbreakServiceException e) {
                        // This will not be thrown as NetworkAttributes is annotated with @JsonIgnoreProperties(ignoreUnknown = true)
                        // cloudResource.getParameterStrict() won't throw an exception either as
                        // even though the strict mapper has FAIL_ON_UNKNOWN_PROPERTIES enabled, that annotation allows silently ignoring it
                        // All fields in NetworkAttributes that are not present in the JSON become null
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                // This filters valid NetworkAttributes only
                .filter(networkAttributes -> StringUtils.isNotBlank(networkAttributes.getNetworkId()))
                .filter(networkAttributes ->
                        Optional.ofNullable(networkAttributes.getOutboundType())
                                .map(OutboundType::isUpgradeable)
                                .orElse(false))
                .findFirst();
    }

    private List<CloudResource> getCloudResources(StackDto stack) {
        Set<ResourceType> resourceTypesToSync = providerSyncConfig.getResourceTypeList();
        return resourceService.getAllCloudResource(stack.getId())
                .stream()
                .filter(cloudResource -> resourceTypesToSync.contains(cloudResource.getType()))
                .collect(Collectors.toList());
    }

    private List<CloudResource> filterSyncedResources(List<CloudResource> allResources, List<CloudResource> syncedResources) {
        Set<String> syncedNames = syncedResources.stream()
                .map(CloudResource::getName)
                .collect(Collectors.toSet());
        return allResources.stream()
                .filter(resource -> !syncedNames.contains(resource.getName()))
                .toList();
    }
}