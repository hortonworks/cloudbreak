package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineDataDisk;
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
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AzureAttachmentResourceBuilder extends AbstractAzureComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAttachmentResourceBuilder.class);

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureInstanceFinder azureInstanceFinder;

    @Override
    public List<CloudResource> create(AzureContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.info("Prepare instance resource to attach to");
        return context.getComputeResources(privateId);
    }

    @Override
    public List<CloudResource> build(AzureContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) {

        CloudResource cloudResourceInstance = azureInstanceFinder.getInstanceCloudResource(privateId, context.getComputeResources(privateId));
        LOGGER.info("Attach disk to the instance {}", cloudResourceInstance);

        CloudContext cloudContext = auth.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack);
        AzureClient client = getAzureClient(auth);
        VirtualMachine vm = client.getVirtualMachineByResourceGroup(resourceGroupName, cloudResourceInstance.getName());
        Set<String> diskIds = vm.dataDisks().values().stream().map(VirtualMachineDataDisk::id).collect(Collectors.toSet());

        CloudResource volumeSet = buildableResource.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AZURE_VOLUMESET))
                .filter(cloudResource -> getVolumeSetAttributes(cloudResource) != null)
                .findFirst()
                .orElseThrow(() -> new AzureResourceException("Volume set resource not found"));

        VolumeSetAttributes volumeSetAttributes = getVolumeSetAttributes(volumeSet);
        LOGGER.debug("Volume set attributes: {}", volumeSetAttributes);
        List<VolumeSetAttributes.Volume> volumes = volumeSetAttributes.getVolumes();
        attachVolumesIfNeeded(client, vm, diskIds, volumes, volumeSetAttributes.getDiscoveryFQDN(), instance.getParameters().get(CloudInstance.FQDN));
        volumeSet.setInstanceId(cloudResourceInstance.getInstanceId());
        volumeSet.setStatus(CommonStatus.CREATED);
        LOGGER.info("Volume set {} attached successfully", volumeSet);
        return List.of(volumeSet);
    }

    private void attachVolumesIfNeeded(AzureClient client, VirtualMachine vm, Set<String> diskIds, List<VolumeSetAttributes.Volume> volumes,
            String volumeFqdn, Object instanceFqdn) {
        List<Disk> disks = new ArrayList<>();
        for (VolumeSetAttributes.Volume volume : volumes) {
            Disk disk = client.getDiskById(volume.getId());
            if (!diskIds.contains(disk.id())) {
                validateVolumeFqdnBeforeAttachment(volumeFqdn, instanceFqdn);
                if (disk.isAttachedToVirtualMachine()) {
                    detachDiskFromVmByVmId(client, disk);
                }
                disks.add(disk);
            } else {
                LOGGER.info("Managed disk {} is already attached to VM {}", disk, vm);
            }
        }
        if (!disks.isEmpty()) {
            attachDisksToVm(client, disks, vm);
        } else {
            LOGGER.info("Disk list is empty");
        }
    }

    private void validateVolumeFqdnBeforeAttachment(String volumeFqdn, Object instanceFqdn) {
        if (volumeFqdn != null && instanceFqdn != null && !StringUtils.equals(volumeFqdn, (String) instanceFqdn)) {
            throw new AzureResourceException(String.format("Not possible to attach volume with FQDN %s to instance with FQDN %s", volumeFqdn, instanceFqdn));
        }
    }

    private void detachDiskFromVmByVmId(AzureClient client, Disk disk) {
        VirtualMachine vm = client.getVirtualMachine(disk.virtualMachineId());
        LOGGER.info("Going to detach disk ([id: {}]) from virtual machine ([name: {}])", disk.id(), vm.name());
        client.detachDiskFromVm(disk.id(), client.getVirtualMachine(disk.virtualMachineId()));
    }

    private void attachDisksToVm(AzureClient client, List<Disk> disks, VirtualMachine vm) {
        LOGGER.info("Going to attach disks ({}) to virtual machine ([name: {}])", disks, vm.name());
        client.attachDisksToVm(disks, vm);
    }

    @Override
    public CloudResource delete(AzureContext context, AuthenticatedContext auth, CloudResource resource) throws PreserveResourceException {
        throw new PreserveResourceException("Resource will be preserved for later reattachment.");
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_VOLUMESET;
    }

    private VolumeSetAttributes getVolumeSetAttributes(CloudResource volumeSet) {
        return volumeSet.getParameterWithFallback(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    private AzureClient getAzureClient(AuthenticatedContext auth) {
        return auth.getParameter(AzureClient.class);
    }

    @Override
    public int order() {
        return 2;
    }
}
