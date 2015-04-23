package com.sequenceiq.cloudbreak.converter;

import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

@Component
public class InstanceGroupToJsonConverter extends AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupJson> {

    @Override
    public InstanceGroupJson convert(InstanceGroup entity) {
        InstanceGroupJson instanceGroupJson = new InstanceGroupJson();
        instanceGroupJson.setGroup(entity.getGroupName());
        instanceGroupJson.setId(entity.getId());
        instanceGroupJson.setNodeCount(entity.getNodeCount());
        instanceGroupJson.setTemplateId(entity.getTemplate().getId());
        instanceGroupJson.setType(entity.getInstanceGroupType());
        instanceGroupJson.setMetadata(convertEntitiesToJson(entity.getInstanceMetaData()));
        return instanceGroupJson;
    }

    private Set<InstanceMetaDataJson> convertEntitiesToJson(Set<InstanceMetaData> metadata) {
        return (Set<InstanceMetaDataJson>) getConversionService().convert(metadata,
                TypeDescriptor.forObject(metadata),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceMetaDataJson.class)));
    }

}
