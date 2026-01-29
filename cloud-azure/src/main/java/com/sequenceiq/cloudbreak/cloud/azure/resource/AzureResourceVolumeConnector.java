package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureVirtualMachineService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureResourceNameService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.RootVolumeFetchDto;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.constant.AzureConstants;
import com.sequenceiq.common.api.type.CommonStatus;

@Service
public class AzureResourceVolumeConnector implements ResourceVolumeConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceVolumeConnector.class);

    private static final String TAG_NAME = "created-for";

    @Inject
    private AzureVolumeResourceBuilder azureVolumeResourceBuilder;

    @Inject
    private AzureAttachmentResourceBuilder azureAttachmentResourceBuilder;

    @Inject
    private AzureResourceNameService resourceNameService;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureVirtualMachineService azureVirtualMachineService;

    @Override
    public void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        LOGGER.debug("Calling detach volumes in AzureVolumeResourceBuilder with resources : {}", cloudResources);
        azureVolumeResourceBuilder.detachVolumes(authenticatedContext, cloudResources);
    }

    @Override
    public void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        LOGGER.debug("Calling delete volumes in AzureVolumeResourceBuilder with resources : {}", cloudResources);
        azureVolumeResourceBuilder.deleteVolumes(authenticatedContext, cloudResources);
    }

    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) {
        LOGGER.info("Calling update volumes in AzureVolumeResourceBuilder for volumes : {} : to disk type : {} and size : {}", volumeIds,
                diskType, size);
        azureVolumeResourceBuilder.modifyVolumes(authenticatedContext, volumeIds, diskType, size);
    }

    @Override
    public List<CloudResource> createVolumes(AuthenticatedContext authenticatedContext, Group group, VolumeSetAttributes.Volume volumeRequest,
            CloudStack cloudStack, int volToAddPerInstance, List<CloudResource> cloudResources) throws CloudbreakServiceException {
        try {
            LOGGER.info("Creating additional volumes with : {} for group: {}", volumeRequest, group.getName());
            if (CollectionUtils.isEmpty(cloudResources)) {
                LOGGER.info("There are no attached EBS volumes in the group. So creating resources!");
                cloudResources = createNewVolumeSets(group, authenticatedContext, cloudStack);
                LOGGER.info("Created {} new VolumeSets", cloudResources.size());
            }
            Map<String, Collection<String>> fqdnToAvailableVolumes = getAvailableVolumes(group, authenticatedContext, cloudStack);
            LOGGER.info("Available Volumes for instances are {}", fqdnToAvailableVolumes);
            cloudResources.forEach(resource -> {
                CloudInstance cloudInstance = group.getInstances().stream().filter(instance ->
                                instance.getInstanceId().equals(resource.getInstanceId())).findFirst()
                        .orElseThrow(() -> new CloudbreakServiceException(format("Instance :%s not found", resource.getInstanceId())));
                String fqdn = cloudInstance.getStringParameter(FQDN);
                VolumeSetAttributes volumeSetAttributes = getVolumeSetAttributes(resource);
                List<VolumeSetAttributes.Volume> volumes = volumeSetAttributes.getVolumes();
                if (volumeSetAttributes.getDiscoveryFQDN() == null) {
                    volumeSetAttributes.setDiscoveryFQDN(fqdn);
                }
                int offset = volumes.size();
                Collection<String> availableVolumes = fqdnToAvailableVolumes.getOrDefault(fqdn, new ArrayList<>());
                LOGGER.info("Available volumes for {} are {}", fqdn, availableVolumes);
                int remainingVolumesToAdd = Math.max(0, volToAddPerInstance - availableVolumes.size());
                addAvailableVolumes(availableVolumes, volumes, volumeRequest);
                addNewVolumes(remainingVolumesToAdd, authenticatedContext, volumeRequest, cloudInstance, group, volumes, cloudStack);
                resource.setStatus(CommonStatus.REQUESTED);
                try {
                    azureVolumeResourceBuilder.build(authenticatedContext, group, List.of(resource), cloudStack, offset, Map.of(TAG_NAME, fqdn));
                } catch (Exception e) {
                    throw new CloudbreakServiceException(e.getMessage());
                }
                resource.setStatus(CommonStatus.CREATED);
            });
            LOGGER.info("Created resources with additional volumes: {}", cloudResources);
            return cloudResources;
        } catch (Exception ex) {
            LOGGER.warn("Exception while creating new volumes: {}", ex.getMessage());
            throw new CloudbreakServiceException(ex.getMessage());
        }
    }

    @Override
    public void attachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources, CloudStack cloudStack)
            throws CloudbreakServiceException {
        cloudResources.stream().forEach(cloudResource -> {
            Group computeGroup = cloudStack.getGroups().stream().filter(group -> group.getName().equals(cloudResource.getGroup())).findFirst()
                    .orElseThrow(() -> new CloudbreakServiceException(format("Instance Group :%s not found for cluster", cloudResource.getGroup())));
            CloudInstance cloudInstance = computeGroup.getInstances().stream()
                    .filter(instance -> instance.getInstanceId().equals(cloudResource.getInstanceId())).findFirst()
                    .orElseThrow(() -> new CloudbreakServiceException(format("Instance :%s not found for cluster", cloudResource.getInstanceId())));
            azureAttachmentResourceBuilder.attachDisks(cloudInstance, authenticatedContext, cloudResource, cloudStack);
        });
    }

    private Map<String, Collection<String>> getAvailableVolumes(Group group, AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        List<String> fqdns = group.getInstances().stream().map(instance -> instance.getStringParameter(FQDN)).collect(Collectors.toList());
        List<Disk> availableDisks = azureAttachmentResourceBuilder.getAvailableDisks(authenticatedContext, cloudStack, TAG_NAME, fqdns);
        return availableDisks.stream().collect(Multimaps.toMultimap(
                disk -> disk.tags().get(TAG_NAME),
                Disk::id,
                ArrayListMultimap::create)).asMap();

    }

    private List<CloudResource> createNewVolumeSets(Group group, AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        List<CloudResource> newVolumeSets = new ArrayList<>();
        for (CloudInstance instance : group.getInstances()) {
            newVolumeSets.add(azureVolumeResourceBuilder.createVolumeSet(instance.getTemplate().getPrivateId(), authenticatedContext, group,
                    CloudResource.builder().withInstanceId(instance.getInstanceId())
                            .withName(instance.getInstanceId())
                            .withType(AZURE_INSTANCE)
                            .build(), cloudStack.getParameters().get(PlatformParametersConsts.RESOURCE_CRN_PARAMETER),
                    false, instance.getStringParameter(FQDN)));
        }
        return newVolumeSets;
    }

    private void addAvailableVolumes(Collection<String> availableVolumes, List<VolumeSetAttributes.Volume> existingVolumes,
            VolumeSetAttributes.Volume volumeRequest) {
        for (String availableVolumeId : availableVolumes) {
            boolean volumeIdDoesNotExist = existingVolumes.stream().map(VolumeSetAttributes.Volume::getId)
                    .noneMatch(volId -> volId.equals(availableVolumeId));
            if (volumeIdDoesNotExist) {
                VolumeSetAttributes.Volume availableVolume = new VolumeSetAttributes.Volume(availableVolumeId,
                        volumeRequest.getDevice(),
                        volumeRequest.getSize(), volumeRequest.getType(),
                        volumeRequest.getCloudVolumeUsageType());
                existingVolumes.add(availableVolume);
            } else {
                LOGGER.info("{} exists in DB", availableVolumeId);
            }
        }
    }

    private void addNewVolumes(int numNewVolumesToAdd, AuthenticatedContext authenticatedContext,
            VolumeSetAttributes.Volume volumeRequest, CloudInstance cloudInstance, Group group, List<VolumeSetAttributes.Volume> existingVolumes,
            CloudStack cloudStack) {
        if (numNewVolumesToAdd > 0) {
            String hashableString = cloudStack.getParameters().get(PlatformParametersConsts.RESOURCE_CRN_PARAMETER)
                    + System.currentTimeMillis();
            List<VolumeSetAttributes.Volume> newVolumes = IntStream.range(0, numNewVolumesToAdd)
                    .mapToObj(num -> {
                        VolumeSetAttributes.Volume vol = new VolumeSetAttributes.Volume(volumeRequest.getId() != null ? volumeRequest.getId() :
                                resourceNameService.attachedDisk(authenticatedContext.getCloudContext().getName(), group.getName(),
                                        cloudInstance.getTemplate().getPrivateId(), existingVolumes.size() + num, hashableString),
                                volumeRequest.getDevice(),
                                volumeRequest.getSize(), volumeRequest.getType(),
                                volumeRequest.getCloudVolumeUsageType());
                        vol.setCloudVolumeStatus(CloudVolumeStatus.REQUESTED);
                        return vol;
                    }).toList();
            existingVolumes.addAll(newVolumes);
        }
    }

    private VolumeSetAttributes getVolumeSetAttributes(CloudResource resource) {
        return resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    @Override
    public List<CloudResource> getRootVolumes(RootVolumeFetchDto rootVolumeFetchDto) {
        AzureClient azureClient = rootVolumeFetchDto.getAuthenticatedContext().getParameter(AzureClient.class);
        return azureCloudResourceService.getAttachedOsDiskResources(rootVolumeFetchDto.getCloudResourceList(),
                rootVolumeFetchDto.getAzureResourceGroupName(), azureClient);
    }

    @Override
    public Map<String, Integer> getAttachedVolumeCountPerInstance(AuthenticatedContext authenticatedContext, CloudStack cloudStack,
            Collection<String> instanceIds) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(authenticatedContext.getCloudContext(), cloudStack);
        Map<String, VirtualMachine> vms = azureVirtualMachineService.getVirtualMachinesByName(client, resourceGroupName, instanceIds);
        return vms.values().stream().collect(Collectors.toMap(VirtualMachine::name, vm -> vm.dataDisks().size()));
    }

    public Map<String, Map<String, String>> getVolumeDeviceMappingByInstance(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(authenticatedContext.getCloudContext(), cloudStack);
        List<String> instanceIds = cloudStack.getGroups().stream()
                .flatMap(g -> g.getInstances().stream())
                .map(CloudInstance::getInstanceId)
                .toList();
        Map<String, VirtualMachine> vmMap = azureVirtualMachineService.getVirtualMachinesByName(client, resourceGroupName, instanceIds);
        return vmMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        vmEntry -> vmEntry.getValue().dataDisks().entrySet().stream()
                                .collect(Collectors.toMap(diskEntry -> diskEntry.getValue().id(),
                                        diskEntry -> AzureConstants.LUN_DEVICE_PATH_PREFIX + diskEntry.getKey()))));
    }
}
