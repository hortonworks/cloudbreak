package com.sequenceiq.cloudbreak.job.provider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
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
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.LoadBalancerSku;
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
        setProviderSyncStatus(stack, syncedCloudResources);
    }

    private void setProviderSyncStatus(StackDto stack, List<CloudResource> syncedCloudResources) {
        boolean hasBasicSku = syncedCloudResources.stream()
                .map(cloudResource -> {
                    try {
                        return cloudResource.getParameter(CloudResource.ATTRIBUTES, SkuAttributes.class);
                    } catch (CloudbreakServiceException e) {
                        return new SkuAttributes();
                    }
                }).anyMatch(skuAttributes -> skuAttributes != null && LoadBalancerSku.BASIC.getTemplateName().equalsIgnoreCase(skuAttributes.getSku()));
        if (hasBasicSku) {
            LOGGER.info("Basic SKU migration is needed for stack, updating status {}", stack.getId());
            stackUpdater.updateProviderState(stack.getId(), Set.of(ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED));
        } else if (CollectionUtils.isNotEmpty(syncedCloudResources)) {
            stackUpdater.updateProviderState(stack.getId(), Set.of(ProviderSyncState.VALID));
        }
    }

    private List<CloudResource> getCloudResources(StackDto stack) {
        Set<ResourceType> resourceTypesToSync = providerSyncConfig.getResourceTypeList();
        return resourceService.getAllCloudResource(stack.getId())
                .stream()
                .filter(cloudResource -> resourceTypesToSync.contains(cloudResource.getType()))
                .collect(Collectors.toList());
    }
}