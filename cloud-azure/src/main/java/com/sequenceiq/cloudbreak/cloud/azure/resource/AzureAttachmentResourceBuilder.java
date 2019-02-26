package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class AzureAttachmentResourceBuilder extends AbstractAzureComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAttachmentResourceBuilder.class);

    @Inject
    private AzureUtils azureUtils;

    @Override
    public List<CloudResource> create(AzureContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.info("Prepare instance resource to attach to");
        return context.getComputeResources(privateId);
    }

    @Override
    public List<CloudResource> build(AzureContext context, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) {
        LOGGER.info("Attach disk to instance");

        CloudResource instance = buildableResource.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AZURE_INSTANCE))
                .findFirst()
                .orElseThrow(() -> new AzureResourceException("Instance resource not found"));

        CloudResource volumeSet = buildableResource.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AZURE_VOLUMESET))
                .findFirst()
                .orElseThrow(() -> new AzureResourceException("Volume set resource not found"));

        CloudContext cloudContext = auth.getCloudContext();
        String resourceGroupName = azureUtils.getResourceGroupName(cloudContext, cloudStack);
        AzureClient client = getAzureClient(auth);

        VolumeSetAttributes volumeSetAttributes = getVolumeSetAttributes(volumeSet);
        volumeSetAttributes.getVolumes()
                .forEach(volume -> {
                            Disk disk = client.getDiskById(volume.getId());
                            VirtualMachine vm = client.getVirtualMachine(resourceGroupName, instance.getName());
                            client.attachDiskToVm(disk, vm);
                        });
        volumeSet.setInstanceId(instance.getInstanceId());
        return List.of(volumeSet);
    }

    @Override
    public CloudResource delete(AzureContext context, AuthenticatedContext auth, CloudResource resource) {
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_VOLUMESET;
    }

    private VolumeSetAttributes getVolumeSetAttributes(CloudResource volumeSet)  {
        return volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    private AzureClient getAzureClient(AuthenticatedContext auth) {
        return auth.getParameter(AzureClient.class);
    }

    @Override
    public int order() {
        return 2;
    }
}