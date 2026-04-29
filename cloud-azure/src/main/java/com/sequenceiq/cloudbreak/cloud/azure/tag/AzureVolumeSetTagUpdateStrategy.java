package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureVolumeSetTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVolumeSetTagUpdateStrategy.class);

    @Inject
    private AzureClientService azureClientService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AZURE_VOLUMESET);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());
        VolumeSetAttributes volumeSetAttributes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);

        if (volumeSetAttributes == null || volumeSetAttributes.getVolumes() == null || volumeSetAttributes.getVolumes().isEmpty()) {
            LOGGER.warn("No volumes found in attributes for AZURE_VOLUMESET: {}", cloudResource.getName());
            return;
        }

        volumeSetAttributes.getVolumes().forEach(volume -> {
            Map<String, String> existingTags = azureClient.getDiskTags(volume.getId());

            if (tagsAlreadyUpToDate(existingTags, tags)) {
                LOGGER.debug("Tags for disk {} are already up to date, skipping update.", cloudResource.getReference());
                return;
            }

            azureClient.updateDiskTags(volume.getId(), mergeTags(existingTags, tags));
        });
    }
}
