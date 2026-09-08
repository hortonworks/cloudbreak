package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
        String resourceId = cloudResource.getReference();
        if (StringUtils.isBlank(resourceId)) {
            LOGGER.warn("Skipping tag update for {} (AZURE_DATABASE): resource reference is null.", cloudResource.getName());
        } else {
            AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());
            String resourceType = ResourceUtils.resourceTypeFromResourceId(resourceId);
            if (FLEXIBLE_SERVER_RESOURCE_TYPE.equalsIgnoreCase(resourceType)) {
                updateServerTags(resourceId, tags, "flexible server", azureClient::getFlexibleServerTags, azureClient::updateFlexibleServerTags);
            } else if (SINGLE_SERVER_RESOURCE_TYPE.equalsIgnoreCase(resourceType)) {
                updateServerTags(resourceId, tags, "single server", azureClient::getSingleServerTags, azureClient::updateSingleServerTags);
            } else {
                LOGGER.debug("Azure database with resourceId {} has not supported DB type", resourceId);
            }
        }
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        String resourceId = cloudResource.getReference();
        if (StringUtils.isBlank(resourceId)) {
            LOGGER.warn("Skipping tag deletion for {} (AZURE_DATABASE): resource reference is null.", cloudResource.getName());
        } else {
            AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());
            String resourceType = ResourceUtils.resourceTypeFromResourceId(resourceId);
            if (FLEXIBLE_SERVER_RESOURCE_TYPE.equalsIgnoreCase(resourceType)) {
                deleteServerTags(resourceId, tagKeys, "flexible server", azureClient::getFlexibleServerTags, azureClient::updateFlexibleServerTags);
            } else if (SINGLE_SERVER_RESOURCE_TYPE.equalsIgnoreCase(resourceType)) {
                deleteServerTags(resourceId, tagKeys, "single server", azureClient::getSingleServerTags, azureClient::updateSingleServerTags);
            } else {
                LOGGER.debug("Azure database with resourceId {} has not supported DB type", resourceId);
            }
        }
    }

    private void updateServerTags(String resourceId, Map<String, String> tags, String serverLabel,
            Function<String, Map<String, String>> tagGetter, BiConsumer<String, Map<String, String>> tagUpdater) {
        Map<String, String> existingTags = tagGetter.apply(resourceId);
        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.debug("Tags for Azure {} {} are already up to date, skipping update.", serverLabel, resourceId);
        } else {
            Map<String, String> mergedTags = mergeTags(existingTags, tags);
            logTagUpdate(LOGGER, resourceId, mergedTags);
            tagUpdater.accept(resourceId, mergedTags);
        }
    }

    private void deleteServerTags(String resourceId, Set<String> tagKeys, String serverLabel,
            Function<String, Map<String, String>> tagGetter, BiConsumer<String, Map<String, String>> tagUpdater) {
        Map<String, String> existingTags = tagGetter.apply(resourceId);
        if (hasTagKeysToDelete(existingTags, tagKeys)) {
            Map<String, String> remainingTags = removeTagKeys(existingTags, tagKeys);
            logTagDeletion(LOGGER, resourceId, tagKeys, existingTags, remainingTags.keySet());
            tagUpdater.accept(resourceId, remainingTags);
        } else {
            LOGGER.debug("No tags to delete for Azure {} {}, skipping.", serverLabel, resourceId);
        }
    }
}
