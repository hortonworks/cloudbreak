package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_DISK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

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
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureVolumeSetTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVolumeSetTagUpdateStrategy.class);

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureSingleResourceTagUpdateStrategy azureSingleResourceTagUpdateStrategy;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AZURE_VOLUMESET);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        forEachVolume(authenticatedContext, cloudResource, "update",
                (azureClient, volumeId) -> azureSingleResourceTagUpdateStrategy.applyTagUpdate(azureClient, AZURE_DISK, volumeId, tags));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        forEachVolume(authenticatedContext, cloudResource, "deletion",
                (azureClient, volumeId) -> azureSingleResourceTagUpdateStrategy.applyTagDeletion(azureClient, AZURE_DISK, volumeId, tagKeys));
    }

    private void forEachVolume(AuthenticatedContext authenticatedContext, CloudResource cloudResource, String operation,
            BiConsumer<AzureClient, String> volumeTagAction) {
        VolumeSetAttributes volumeSetAttributes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        if (hasNoVolumes(volumeSetAttributes)) {
            LOGGER.warn("No volumes found in attributes for AZURE_VOLUMESET: {}", cloudResource.getName());
        } else {
            AzureClient azureClient = azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());
            volumeSetAttributes.getVolumes().forEach(volume -> {
                String volumeId = volume.getId();
                if (StringUtils.isBlank(volumeId)) {
                    LOGGER.warn("Skipping tag {} for a volume in AZURE_VOLUMESET {}: volume ID is null.", operation, cloudResource.getName());
                } else {
                    volumeTagAction.accept(azureClient, volumeId);
                }
            });
        }
    }

    private boolean hasNoVolumes(VolumeSetAttributes volumeSetAttributes) {
        return volumeSetAttributes == null || volumeSetAttributes.getVolumes() == null || volumeSetAttributes.getVolumes().isEmpty();
    }
}
