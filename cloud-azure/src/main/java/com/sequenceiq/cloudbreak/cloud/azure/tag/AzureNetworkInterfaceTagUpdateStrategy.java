package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK_INTERFACE;

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
public class AzureNetworkInterfaceTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNetworkInterfaceTagUpdateStrategy.class);

    @Inject
    private AzureClientService azureClientService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AZURE_NETWORK_INTERFACE);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        String reference = cloudResource.getReference();
        if (StringUtils.isBlank(reference)) {
            LOGGER.warn("Skipping tag update for {} (AZURE_NETWORK_INTERFACE): resource reference is null.",
                    cloudResource.getName());
            return;
        }
        AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());

        Map<String, String> existingTags = azureClient.getNetworkInterfaceTags(reference);
        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.debug("Tags for network interface {} are already up to date, skipping update.", reference);
            return;
        }

        azureClient.updateNetworkInterfaceTags(reference, mergeTags(existingTags, tags));
    }
}
