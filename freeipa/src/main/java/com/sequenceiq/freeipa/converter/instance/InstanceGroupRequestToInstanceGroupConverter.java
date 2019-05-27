package com.sequenceiq.freeipa.converter.instance;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.converter.instance.template.InstanceTemplateRequestToTemplateConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

@Component
public class InstanceGroupRequestToInstanceGroupConverter {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private InstanceTemplateRequestToTemplateConverter templateConverter;

    @Inject
    private SecurityGroupRequestToSecurityGroupConverter securityGroupConverter;

    public InstanceGroup convert(InstanceGroupRequest source, String cloudPlatformString) {
        InstanceGroup instanceGroup = new InstanceGroup();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(cloudPlatformString);
        instanceGroup.setTemplate(templateConverter.convert(source.getInstanceTemplate(), cloudPlatform));
        instanceGroup.setSecurityGroup(securityGroupConverter.convert(source.getSecurityGroup()));
        instanceGroup.setGroupName(source.getName());
        instanceGroup.setInstanceGroupType(source.getType());
        if (source.getNodeCount() > 0) {
            addInstanceMetadatas(source, instanceGroup);
        }
        return instanceGroup;
    }

    private void addInstanceMetadatas(InstanceGroupRequest request, InstanceGroup instanceGroup) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < request.getNodeCount(); i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaDataSet.add(instanceMetaData);
        }
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
    }
}
