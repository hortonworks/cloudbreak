package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_AVAILABILITY_SET;

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
public class AzureAvailabilitySetTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAvailabilitySetTagUpdateStrategy.class);

    @Inject
    private AzureClientService azureClientService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AZURE_AVAILABILITY_SET);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        String reference = cloudResource.getReference();
        if (StringUtils.isBlank(reference)) {
            LOGGER.warn("Skipping tag update for {} (AZURE_AVAILABILITY_SET): resource reference is null.",
                    cloudResource.getName());
            return;
        }
        AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());

        Map<String, String> existingTags = azureClient.getAvailabilitySetTags(reference);
        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.debug("Tags for availability set {} are already up to date, skipping update.", reference);
            return;
        }

        azureClient.updateAvailabilitySetTags(reference, mergeTags(existingTags, tags));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        String reference = cloudResource.getReference();
        if (StringUtils.isBlank(reference)) {
            LOGGER.warn("Skipping tag deletion for {} (AZURE_AVAILABILITY_SET): resource reference is null.",
                    cloudResource.getName());
            return;
        }
        AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());

        Map<String, String> existingTags = azureClient.getAvailabilitySetTags(reference);
        if (!hasTagKeysToDelete(existingTags, tagKeys)) {
            LOGGER.debug("No tags to delete for availability set {}, skipping.", reference);
            return;
        }

        Map<String, String> remainingTags = removeTagKeys(existingTags, tagKeys);
        logTagDeletion(LOGGER, reference, tagKeys, existingTags, remainingTags.keySet());
        azureClient.updateAvailabilitySetTags(reference, remainingTags);
    }
}
