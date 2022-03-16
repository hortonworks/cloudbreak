package com.sequenceiq.freeipa.converter.instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.VolumeDetails;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Template;

@Component
public class InstanceGroupToInstanceGroupDetailsConverter {

    private static final String ENCRYPTED = "encrypted";

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
            VolumeDetails volumeDetails = new VolumeDetails();
            volumeDetails.setVolumeType(template.getVolumeType());
            volumeDetails.setVolumeSize(template.getVolumeSize());
            volumeDetails.setVolumeCount(template.getVolumeCount());
            instanceGroupDetails.setVolumes(List.of(volumeDetails));
        }
        return instanceGroupDetails;
    }

    private Map<String, Object> filterAttributes(Map<String, Object> input) {
        Map<String, Object> attributes = new HashMap<>();
        if (input != null && input.containsKey(ENCRYPTED)) {
            attributes.put(ENCRYPTED, input.get(ENCRYPTED));
        }
        return attributes;
    }
}
