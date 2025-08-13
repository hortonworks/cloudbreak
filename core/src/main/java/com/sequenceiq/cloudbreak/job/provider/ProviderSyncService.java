package com.sequenceiq.cloudbreak.job.provider;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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

    public void syncResources(StackDto stack) {
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
        resourceNotifier.notifyUpdates(syncedCloudResources, cloudContext);
        setProviderSyncStatus(stack, syncedCloudResources, cloudResources);
    }

    private void setProviderSyncStatus(StackDto stack, List<CloudResource> syncedCloudResources, List<CloudResource> cloudResources) {
        Optional<SkuAttributes> hasBasicSku = hasBasicSku(syncedCloudResources);
        if (hasBasicSku.isPresent()) {
            LOGGER.info("Basic SKU migration is needed for {}, updating status", hasBasicSku.get());
            stackUpdater.addProviderState(stack.getId(), ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED);
        } else if (shouldUpgradeOutbound(syncedCloudResources).isPresent() || shouldUpgradeOutbound(cloudResources).isPresent()) {
            LOGGER.info("Outbound upgrade is needed for {}, updating status",
                    shouldUpgradeOutbound(syncedCloudResources).or(() -> shouldUpgradeOutbound(cloudResources)));
            stackUpdater.addProviderState(stack.getId(), ProviderSyncState.OUTBOUND_UPGRADE_NEEDED);
        } else {
            LOGGER.debug("Provider sync have not detected errors for {}, cleaning up error states",
                    syncedCloudResources.stream().map(CloudResource::getDetailedInfo).toList());
            stackUpdater.removeProviderStates(stack.getId(), Set.of(ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED, ProviderSyncState.OUTBOUND_UPGRADE_NEEDED));
        }
    }

    private Optional<SkuAttributes> hasBasicSku(List<CloudResource> syncedCloudResources) {
        return syncedCloudResources.stream()
                .map(cloudResource -> {
                    try {
                        return cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class);
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
                        return cloudResource.getParameter(CloudResource.ATTRIBUTES, NetworkAttributes.class);
                    } catch (CloudbreakServiceException e) {
                        return new NetworkAttributes();
                    }
                }).filter(networkAttributes ->
                        Optional.ofNullable(networkAttributes)
                                .map(NetworkAttributes::getOutboundType)
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
}