package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceUtils;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureDatabaseResourceTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseResourceTagUpdateStrategy.class);

    private static final String FLEXIBLE_SERVER_RESOURCE_TYPE = "flexibleServers";

    private static final String SINGLE_SERVER_RESOURCE_TYPE = "servers";

    @Inject
    private AzureClientService azureClientService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AZURE_DATABASE);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());
        String resourceId = cloudResource.getReference();
        String resourceType = ResourceUtils.resourceTypeFromResourceId(resourceId);

        if (FLEXIBLE_SERVER_RESOURCE_TYPE.equalsIgnoreCase(resourceType)) {
            updateFlexibleServerTags(azureClient, resourceId, tags);
        } else if (SINGLE_SERVER_RESOURCE_TYPE.equalsIgnoreCase(resourceType)) {
            updateSingleServerTags(azureClient, resourceId, tags);
        } else {
            LOGGER.debug("Azure database with resourceId {} has not supported DB type", resourceId);
        }
    }

    private void updateFlexibleServerTags(AzureClient azureClient, String resourceId, Map<String, String> tags) {
        Map<String, String> existingTags = azureClient.getFlexibleServerTags(resourceId);

        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.debug("Tags for Azure flexible server {} are already up to date, skipping update.", resourceId);
            return;
        }

        Map<String, String> mergedTags = mergeTags(existingTags, tags);
        LOGGER.debug("Updating flexible server tags for {} with tags {}", resourceId, mergedTags);
        azureClient.updateFlexibleServerTags(resourceId, mergedTags);
    }

    private void updateSingleServerTags(AzureClient azureClient, String resourceId, Map<String, String> tags) {
        Map<String, String> existingTags = azureClient.getSingleServerTags(resourceId);

        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.debug("Tags for Azure single server {} are already up to date, skipping update.", resourceId);
            return;
        }

        Map<String, String> mergedTags = mergeTags(existingTags, tags);
        LOGGER.debug("Updating single server tags for {} with tags {}", resourceId, mergedTags);
        azureClient.updateSingleServerTags(resourceId, mergedTags);
    }
}
