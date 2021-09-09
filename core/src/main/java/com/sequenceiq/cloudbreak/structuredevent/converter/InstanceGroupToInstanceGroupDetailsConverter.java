package com.sequenceiq.cloudbreak.structuredevent.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.VolumeDetails;

@Component
public class InstanceGroupToInstanceGroupDetailsConverter {

    @Inject
    private SecurityGroupToSecurityGroupDetailsConverter securityGroupToSecurityGroupDetailsConverter;

    public InstanceGroupDetails convert(InstanceGroup source) {
        InstanceGroupDetails instanceGroupDetails = new InstanceGroupDetails();
        instanceGroupDetails.setGroupName(source.getGroupName());
        instanceGroupDetails.setGroupType(source.getInstanceGroupType().name());
        instanceGroupDetails.setNodeCount(source.getNodeCount());
        Template template = source.getTemplate();
        if (template != null) {
            instanceGroupDetails.setInstanceType(template.getInstanceType());
            instanceGroupDetails.setAttributes(template.getAttributes().getMap());
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
        instanceGroupDetails.setSecurityGroup(getIfNotNull(source.getSecurityGroup(), securityGroupToSecurityGroupDetailsConverter::convert));
        return instanceGroupDetails;
    }
}
