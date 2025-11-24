package com.sequenceiq.cloudbreak.service.stack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class DiskValidationService {

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

    public List<CloudResource> getVolumesForValidation(StackView stack, Map<String, Set<String>> repairableGroupsWithHostNames) {
        List<Resource> resources = resourceService
                .findAllByStackIdAndResourceTypeIn(stack.getId(), getResourceTypes(stack.getPlatformVariant(), stack.getDiskResourceType()));
        List<CloudResource> resourceToCheck = new ArrayList<>();
        for (Map.Entry<String, Set<String>> groupAndHosts : repairableGroupsWithHostNames.entrySet()) {
            Set<String> repairableHosts = groupAndHosts.getValue();
            for (String hostName : repairableHosts) {
                Optional<InstanceMetaData> instance = instanceMetaDataService.findHostInStack(stack.getId(), hostName);
                if (instance.isPresent()) {
                    resourceToCheck.addAll(getDisksForInstance(resources, stack, instance.get()));
                }
            }
        }
        addSyncerResourceTypes(stack.getDiskResourceType(), stack.getPlatformVariant(), resourceToCheck, resources);
        return resourceToCheck;
    }

    private List<CloudResource> getDisksForInstance(List<Resource> resources, StackView stack, InstanceMetaData instance) {
        return resources.stream()
                .filter(resource -> resource.getResourceType() == stack.getDiskResourceType())
                .filter(resource -> instance.getInstanceId().equalsIgnoreCase(resource.getInstanceId()))
                .filter(resource -> !resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class)
                        .map(VolumeSetAttributes::getDeleteOnTermination).orElse(Boolean.TRUE))
                .map(resourceToCloudResourceConverter::convert)
                .toList();
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
}
