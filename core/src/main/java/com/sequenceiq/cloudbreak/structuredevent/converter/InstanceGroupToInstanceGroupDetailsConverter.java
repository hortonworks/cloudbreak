package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.VolumeDetails;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class InstanceGroupToInstanceGroupDetailsConverter {

    public InstanceGroupDetails convert(InstanceGroupView source, List<InstanceMetadataView> instanceMetadatas) {
        InstanceGroupDetails instanceGroupDetails = new InstanceGroupDetails();
        instanceGroupDetails.setGroupName(source.getGroupName());
        instanceGroupDetails.setGroupType(source.getInstanceGroupType().name());
        instanceGroupDetails.setNodeCount(instanceMetadatas.size());
        Template template = source.getTemplate();
        if (template != null) {
            instanceGroupDetails.setInstanceType(template.getInstanceType());
            instanceGroupDetails.setAttributes(filterAttributes(template.getAttributes().getMap()));
            instanceGroupDetails.setRootVolumeSize(template.getRootVolumeSize());
            if (template.getVolumeTemplates() != null) {
                instanceGroupDetails.setVolumes(template.getVolumeTemplates().stream().map(volmue -> {
                    VolumeDetails volumeDetails = new VolumeDetails();
                    volumeDetails.setVolumeType(volmue.getVolumeType());
                    volumeDetails.setVolumeSize(volmue.getVolumeSize());
                    volumeDetails.setVolumeCount(volmue.getVolumeCount());
                    return volumeDetails;
                }).collect(Collectors.toList()));
            }
            if (template.getTemporaryStorage() != null) {
                instanceGroupDetails.setTemporaryStorage(template.getTemporaryStorage().name());
            }
        }
        instanceGroupDetails.setRunningInstances(getRunningInstances(instanceMetadatas, template != null ? template.getInstanceType() : "unknown"));
        return instanceGroupDetails;
    }

    private Map<String, Object> filterAttributes(Map<String, Object> input) {
        Map<String, Object> attributes;
        if (input != null) {
            attributes = input.entrySet().stream()
                    .filter(e -> "encrypted".equals(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    private Map<String, Long> getRunningInstances(List<InstanceMetadataView> instanceMetadatas, String templateInstanceType) {
        return instanceMetadatas.stream()
                .filter(instance -> StackDtoToMeteringEventConverter.REPORTING_INSTANCE_STATUSES.contains(instance.getInstanceStatus()))
                .collect(Collectors.groupingBy(instance -> getInstanceType(instance.getProviderInstanceType(), templateInstanceType), Collectors.counting()));
    }

    private String getInstanceType(String providerInstanceType, String templateInstanceType) {
        return providerInstanceType != null ? providerInstanceType : templateInstanceType;
    }
}
