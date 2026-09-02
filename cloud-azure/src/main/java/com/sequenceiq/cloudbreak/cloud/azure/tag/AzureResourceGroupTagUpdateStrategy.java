package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureResourceGroupTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceGroupTagUpdateStrategy.class);

    @Inject
    private AzureClientService azureClientService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AZURE_RESOURCE_GROUP);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        String name = cloudResource.getName();
        if (StringUtils.isBlank(name)) {
            LOGGER.warn("Skipping tag update for resource group: resource name is null.");
            return;
        }
        AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());

        Map<String, String> existingTags = azureClient.getResourceGroupTags(name);
        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.debug("Tags for resource group {} are already up to date, skipping update.", name);
            return;
        }

        azureClient.updateResourceGroupTags(name, mergeTags(existingTags, tags));
    }
}
