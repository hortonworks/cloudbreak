package com.sequenceiq.cloudbreak.converter.v2;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class InstanceGroupV2RequestToInstanceGroupConverter
        extends AbstractConversionServiceAwareConverter<InstanceGroupV2Request, InstanceGroup> {

    @Override
    public InstanceGroup convert(InstanceGroupV2Request instanceGroupV2Request) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(getConversionService().convert(instanceGroupV2Request.getTemplate(), Template.class));
        instanceGroup.setSecurityGroup(getConversionService().convert(instanceGroupV2Request.getSecurityGroup(), SecurityGroup.class));
        instanceGroup.setGroupName(instanceGroupV2Request.getGroup());
        setAttributes(instanceGroupV2Request, instanceGroup);
        instanceGroup.setInstanceGroupType(instanceGroupV2Request.getType());
        if (instanceGroupV2Request.getNodeCount() > 0) {
            addInstanceMetadatas(instanceGroupV2Request, instanceGroup);
        }
        return instanceGroup;
    }

    private void addInstanceMetadatas(InstanceGroupV2Request request, InstanceGroup instanceGroup) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < request.getNodeCount(); i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaDataSet.add(instanceMetaData);
        }
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
    }

    private void setAttributes(InstanceGroupV2Request json, InstanceGroup instanceGroup) {
        try {
            Json jsonProperties = new Json(json.getParameters());
            instanceGroup.setAttributes(jsonProperties);
        } catch (JsonProcessingException ignored) {
            instanceGroup.setAttributes(null);
        }
    }
}
