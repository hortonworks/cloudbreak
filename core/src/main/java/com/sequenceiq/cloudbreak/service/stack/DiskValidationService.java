package com.sequenceiq.cloudbreak.service.stack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class DiskValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskValidationService.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private List<ProviderResourceSyncer> providerResourceSyncers;

    public DiskValidationVolumes getVolumesForValidation(StackDtoDelegate stackDto, Map<String, Set<String>> repairableGroupsWithHostNames) {
        StackView stack = stackDto.getStack();
        boolean embeddedDatabaseOnAttachedDisk = isEmbeddedDatabaseOnAttachedDisk(stackDto.getCluster());
        List<Resource> resources = resourceService
                .findAllByStackIdAndResourceTypeIn(stack.getId(), getResourceTypes(stack.getPlatformVariant(), stack.getDiskResourceType()));
        List<CloudResource> resourceToCheck = new ArrayList<>();
        List<CloudResource> embeddedDatabaseVolumes = new ArrayList<>();
        Set<String> clusterManagerServerWithEmbeddedDbInstanceIds = new HashSet<>();
        for (Map.Entry<String, Set<String>> groupAndHosts : repairableGroupsWithHostNames.entrySet()) {
            Set<String> repairableHosts = groupAndHosts.getValue();
            for (String hostName : repairableHosts) {
                Optional<InstanceMetaData> instance = instanceMetaDataService.findHostInStack(stack.getId(), hostName);
                if (instance.isPresent()) {
                    List<Resource> volumeSets = getVolumeSetsOfInstance(resources, stack, instance.get());
                    resourceToCheck.addAll(getDisksForInstance(volumeSets));
                    if (embeddedDatabaseOnAttachedDisk && Boolean.TRUE.equals(instance.get().getClusterManagerServer())) {
                        List<CloudResource> databaseVolumeSets = getEmbeddedDatabaseVolumesForInstance(volumeSets, instance.get());
                        databaseVolumeSets.stream()
                                .map(CloudResource::getInstanceId)
                                .filter(Objects::nonNull)
                                .forEach(clusterManagerServerWithEmbeddedDbInstanceIds::add);
                        embeddedDatabaseVolumes.addAll(databaseVolumeSets);
                    }
                }
            }
        }
        if (!embeddedDatabaseVolumes.isEmpty()) {
            addSyncerResourceTypes(stack.getDiskResourceType(), stack.getPlatformVariant(), embeddedDatabaseVolumes, resources);
        }
        addSyncerResourceTypes(stack.getDiskResourceType(), stack.getPlatformVariant(), resourceToCheck, resources);
        return new DiskValidationVolumes(resourceToCheck, embeddedDatabaseVolumes, clusterManagerServerWithEmbeddedDbInstanceIds);
    }

    private boolean isEmbeddedDatabaseOnAttachedDisk(ClusterView cluster) {
        return cluster != null && Boolean.TRUE.equals(cluster.getEmbeddedDatabaseOnAttachedDisk());
    }

    // Matched by FQDN as well, like ClusterRepairService, because after a half-finished repair the volume set still has the old instance id
    private List<Resource> getVolumeSetsOfInstance(List<Resource> resources, StackView stack, InstanceMetaData instance) {
        return resources.stream()
                .filter(resource -> resource.getResourceType() == stack.getDiskResourceType())
                .filter(resource -> isVolumeSetOfInstance(resource, instance))
                .toList();
    }

    private boolean isVolumeSetOfInstance(Resource volumeSet, InstanceMetaData instance) {
        return (instance.getInstanceId() != null && instance.getInstanceId().equalsIgnoreCase(volumeSet.getInstanceId()))
                || (instance.getDiscoveryFQDN() != null && instance.getDiscoveryFQDN().equals(resourceAttributeUtil
                        .getTypedAttributes(volumeSet, VolumeSetAttributes.class).map(VolumeSetAttributes::getDiscoveryFQDN).orElse(null)));
    }

    private List<CloudResource> getDisksForInstance(List<Resource> volumeSets) {
        return volumeSets.stream()
                .filter(resource -> !resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class)
                        .map(VolumeSetAttributes::getDeleteOnTermination).orElse(Boolean.TRUE))
                .map(resourceToCloudResourceConverter::convert)
                .toList();
    }

    // Checked even when the volume set is marked for deletion, because recreating the disk mounted to /dbfs loses the embedded CM database
    private List<CloudResource> getEmbeddedDatabaseVolumesForInstance(List<Resource> volumeSets, InstanceMetaData instance) {
        List<CloudResource> result = volumeSets.stream()
                .map(this::toDatabaseOnlyVolumeSet)
                .flatMap(Optional::stream)
                .toList();
        if (result.isEmpty()) {
            LOGGER.warn("No DATABASE usage type volume found for the Cloudera Manager server instance {}, so the embedded database disk check is " +
                    "skipped for it", instance.getInstanceId());
        } else {
            LOGGER.info("Checking the embedded database volume set(s) {} of the Cloudera Manager server instance {}",
                    result.stream().map(CloudResource::getName).toList(), instance.getInstanceId());
        }
        return result;
    }

    private Optional<CloudResource> toDatabaseOnlyVolumeSet(Resource volumeSet) {
        return resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class).flatMap(attributes -> {
            List<VolumeSetAttributes.Volume> databaseVolumes = attributes.getVolumes().stream()
                    .filter(volume -> volume.getCloudVolumeUsageType() == CloudVolumeUsageType.DATABASE)
                    .toList();
            if (databaseVolumes.isEmpty()) {
                return Optional.empty();
            }
            CloudResource databaseVolumeSet = resourceToCloudResourceConverter.convert(volumeSet);
            databaseVolumeSet.setTypedAttributes(new VolumeSetAttributes(attributes.getAvailabilityZone(), attributes.getDeleteOnTermination(),
                    attributes.getFstab(), attributes.getUuids(), databaseVolumes, attributes.getDiscoveryFQDN()));
            return Optional.of(databaseVolumeSet);
        });
    }

    private List<ResourceType> getResourceTypes(String platformVariant, ResourceType diskResourceType) {
        List<ResourceType> result = new ArrayList<>();
        result.add(diskResourceType);
        result.addAll(getRequiredResourceTypesFromSyncer(diskResourceType));
        return result;
    }

    private List<ResourceType> getRequiredResourceTypesFromSyncer(ResourceType diskResourceType) {
        return providerResourceSyncers.stream()
                .filter(syncer -> syncer.getResourceType() == diskResourceType)
                .findFirst()
                .map(ProviderResourceSyncer::getRequiredResourceTypes)
                .orElse(List.of());
    }

    private void addSyncerResourceTypes(ResourceType diskResourceType, String platformVariant, List<CloudResource> resourceToCheck,
            List<Resource> resources) {
        Set<ResourceType> requiredResourceTypesFromSyncer = new HashSet<>(getRequiredResourceTypesFromSyncer(diskResourceType));
        if (!requiredResourceTypesFromSyncer.isEmpty()) {
            resources.stream()
                    .filter(resource -> requiredResourceTypesFromSyncer.contains(resource.getResourceType()))
                    .forEach(resource -> resourceToCheck.add(resourceToCloudResourceConverter.convert(resource)));
        }
    }

    public record DiskValidationVolumes(List<CloudResource> volumes, List<CloudResource> embeddedDatabaseVolumes,
            Set<String> clusterManagerServerWithEmbeddedDbInstanceIds) {
    }
}
