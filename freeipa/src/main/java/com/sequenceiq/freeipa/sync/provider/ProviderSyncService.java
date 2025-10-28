package com.sequenceiq.freeipa.sync.provider;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
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
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Service
public class ProviderSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncService.class);

    @Inject
    private ProviderSyncConfig providerSyncConfig;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceNotifier resourceNotifier;

    public void syncResources(Stack stack) {
        try {
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = CloudContext.Builder.builder()
                    .withId(stack.getId())
                    .withName(stack.getName())
                    .withCrn(stack.getResourceCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withVariant(stack.getPlatformvariant())
                    .withLocation(location)
                    .withUserName(stack.getOwner())
                    .withAccountId(stack.getAccountId())
                    .build();

            List<CloudResource> cloudResources = getCloudResources(stack);
            LOGGER.debug("Syncing resources for stack: {}", cloudResources.stream()
                    .map(CloudResource::getDetailedInfo)
                    .toList());

            CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
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
        } catch (Exception e) {
            LOGGER.error("Error during provider sync, skipping and logging it: ", e);
        }
    }

    private List<CloudResource> getCloudResources(Stack stack) {
        Set<ResourceType> resourceTypesToSync = providerSyncConfig.getResourceTypeList();
        return resourceService.getAllCloudResource(stack.getId()).stream()
                .filter(cloudResource -> resourceTypesToSync.contains(cloudResource.getType()))
                .collect(Collectors.toList());
    }

}