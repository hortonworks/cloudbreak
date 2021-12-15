package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.VolumeDetails;

@Component
public class InstanceGroupToInstanceGroupDetailsConverter {

    public InstanceGroupDetails convert(InstanceGroup source) {
        InstanceGroupDetails instanceGroupDetails = new InstanceGroupDetails();
        instanceGroupDetails.setGroupName(source.getGroupName());
        instanceGroupDetails.setGroupType(source.getInstanceGroupType().name());
        instanceGroupDetails.setNodeCount(source.getNodeCount());
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
}
