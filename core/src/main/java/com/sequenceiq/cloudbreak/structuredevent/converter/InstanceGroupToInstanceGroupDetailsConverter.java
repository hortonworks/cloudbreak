package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.SecurityGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.VolumeDetails;

@Component
public class InstanceGroupToInstanceGroupDetailsConverter extends AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupDetails>  {

    @Override
    public InstanceGroupDetails convert(InstanceGroup source) {
        InstanceGroupDetails instanceGroupDetails = new InstanceGroupDetails();
        instanceGroupDetails.setGroupName(source.getGroupName());
        instanceGroupDetails.setGroupType(source.getInstanceGroupType().name());
        instanceGroupDetails.setNodeCount(source.getNodeCount());
        Template template = source.getTemplate();
        if (template != null) {
            instanceGroupDetails.setInstanceType(source.getTemplate().getInstanceType());
            instanceGroupDetails.setVolumes(source.getTemplate().getVolumeTemplates().stream().map(volmue -> {
                VolumeDetails volumeDetails = new VolumeDetails();
                volumeDetails.setVolumeType(volmue.getVolumeType());
                volumeDetails.setVolumeSize(volmue.getVolumeSize());
                volumeDetails.setVolumeCount(volmue.getVolumeCount());
                return volumeDetails;
            }).collect(Collectors.toList()));
        }
        instanceGroupDetails.setSecurityGroup(getConversionService().convert(source.getSecurityGroup(), SecurityGroupDetails.class));
        return instanceGroupDetails;
    }
}
