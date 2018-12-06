package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.SecurityGroupDetails;

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
            instanceGroupDetails.setVolumeType(source.getTemplate().getVolumeType());
            instanceGroupDetails.setVolumeSize(source.getTemplate().getVolumeSize());
            instanceGroupDetails.setVolumeCount(source.getTemplate().getVolumeCount());
        }
        instanceGroupDetails.setSecurityGroup(getConversionService().convert(source.getSecurityGroup(), SecurityGroupDetails.class));
        return instanceGroupDetails;
    }
}
