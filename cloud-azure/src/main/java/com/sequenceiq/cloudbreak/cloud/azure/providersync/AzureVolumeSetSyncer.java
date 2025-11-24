package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContextBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureVolumeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureVolumeSetSyncer implements ProviderResourceSyncer<ResourceType> {

    @Inject
    private AzureVolumeResourceBuilder azureVolumeResourceBuilder;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private AzureContextBuilder contextBuilder;

    @Override
    public List<CloudResourceStatus> sync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        AzureContext context = contextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        List<CloudResource> volumeSets = resources.stream()
                .filter(resource -> resource.getType() == getResourceType())
                .toList();
        context.addNetworkResources(azureCloudResourceService.getNetworkResources(resources));
        for (CloudResource volumeSet : volumeSets) {
            result.addAll(azureVolumeResourceBuilder.checkResources(context, authenticatedContext, List.of(volumeSet)));
        }
        return result;
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.AZURE_VOLUMESET;
    }

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }

    @Override
    public List<ResourceType> getRequiredResourceTypes() {
        return List.of(ResourceType.AZURE_RESOURCE_GROUP);
    }
}
