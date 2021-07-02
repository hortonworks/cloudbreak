package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineDataDisk;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AzureAttachmentResourceBuilder extends AbstractAzureComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAttachmentResourceBuilder.class);

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Override
    public List<CloudResource> create(AzureContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.info("Prepare instance resource to attach to");
        return context.getComputeResources(privateId);
    }

    @Override
    public List<CloudResource> build(AzureContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) {

        CloudResource cloudResourceInstance = buildableResource.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AZURE_INSTANCE))
                .findFirst()
                .orElseThrow(() -> new AzureResourceException("Instance resource not found"));

        LOGGER.info("Attach disk to the instance {}", instance);

        CloudContext cloudContext = auth.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack);
        AzureClient client = getAzureClient(auth);
        VirtualMachine vm = client.getVirtualMachineByResourceGroup(resourceGroupName, cloudResourceInstance.getName());
        Set<String> diskIds = vm.dataDisks().values().stream().map(VirtualMachineDataDisk::id).collect(Collectors.toSet());

        CloudResource volumeSet = buildableResource.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AZURE_VOLUMESET))
                .filter(cloudResource -> !instance.getInstanceId().equals(cloudResource.getInstanceId()))
                .findFirst()
                .orElseThrow(() -> new AzureResourceException("Volume set resource not found"));

        VolumeSetAttributes volumeSetAttributes = getVolumeSetAttributes(volumeSet);
        volumeSetAttributes.getVolumes()
                .forEach(volume -> {
                    Disk disk = client.getDiskById(volume.getId());
                    if (!diskIds.contains(disk.id())) {
                        if (disk.isAttachedToVirtualMachine()) {
                            detachDiskFromVmByVmId(client, disk);
                        }
                        attachDiskToVm(client, disk, vm);
                    } else {
                        LOGGER.info("Managed disk {} is already attached to VM {}", disk, vm);
                    }
                });
        volumeSet.setInstanceId(instance.getInstanceId());
        volumeSet.setStatus(CommonStatus.CREATED);
        LOGGER.info("Volume set {} attached successfully", volumeSet);
        return List.of(volumeSet);
    }

    private void detachDiskFromVmByVmId(AzureClient client, Disk disk) {
        VirtualMachine vm = client.getVirtualMachine(disk.virtualMachineId());
        LOGGER.info("Going to detach disk ([id: {}]) from virtual machine ([id: {}])", disk.id(), vm.vmId());
        client.detachDiskFromVm(disk.id(), client.getVirtualMachine(disk.virtualMachineId()));
    }

    private void attachDiskToVm(AzureClient client, Disk disk, VirtualMachine vm) {
        LOGGER.info("Going to attach disk ([id: {}]) to virtual machine ([id: {}])", disk.id(), vm.vmId());
        client.attachDiskToVm(disk, vm);
    }

    @Override
    public CloudResource delete(AzureContext context, AuthenticatedContext auth, CloudResource resource) throws InterruptedException {
        throw new InterruptedException("Resource will be preserved for later reattachment.");
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_VOLUMESET;
    }

    private VolumeSetAttributes getVolumeSetAttributes(CloudResource volumeSet) {
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
