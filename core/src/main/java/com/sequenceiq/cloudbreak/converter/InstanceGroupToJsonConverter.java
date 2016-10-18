package com.sequenceiq.cloudbreak.converter;

import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

@Component
public class InstanceGroupToJsonConverter extends AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupResponse> {

    @Override
    public InstanceGroupResponse convert(InstanceGroup entity) {
        InstanceGroupResponse instanceGroupResponse = new InstanceGroupResponse();
        instanceGroupResponse.setGroup(entity.getGroupName());
        instanceGroupResponse.setId(entity.getId());
        instanceGroupResponse.setNodeCount(entity.getNodeCount());
        instanceGroupResponse.setTemplateId(entity.getTemplate().getId());
        instanceGroupResponse.setType(entity.getInstanceGroupType());
        instanceGroupResponse.setMetadata(convertEntitiesToJson(entity.getInstanceMetaData()));
        if (entity.getSecurityGroup() != null) {
            instanceGroupResponse.setSecurityGroupId(entity.getSecurityGroup().getId());
        }
        return instanceGroupResponse;
    }

    private Set<InstanceMetaDataJson> convertEntitiesToJson(Set<InstanceMetaData> metadata) {
        return (Set<InstanceMetaDataJson>) getConversionService().convert(metadata,
                TypeDescriptor.forObject(metadata),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceMetaDataJson.class)));
    }

}
