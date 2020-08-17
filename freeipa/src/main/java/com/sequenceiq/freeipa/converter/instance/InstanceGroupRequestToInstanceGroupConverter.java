package com.sequenceiq.freeipa.converter.instance;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.converter.instance.template.InstanceTemplateRequestToTemplateConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;

@Component
public class InstanceGroupRequestToInstanceGroupConverter {

    @Inject
    private InstanceTemplateRequestToTemplateConverter templateConverter;

    @Inject
    private SecurityGroupRequestToSecurityGroupConverter securityGroupConverter;

    @Inject
    private DefaultInstanceGroupProvider defaultInstanceGroupProvider;

    public InstanceGroup convert(InstanceGroupRequest source, String accountId, String cloudPlatformString, String stackName) {
        InstanceGroup instanceGroup = new InstanceGroup();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(cloudPlatformString);
        instanceGroup.setTemplate(source.getInstanceTemplate() == null
                ? defaultInstanceGroupProvider.createDefaultTemplate(cloudPlatform, accountId)
                : templateConverter.convert(source.getInstanceTemplate(), cloudPlatform, accountId));
        instanceGroup.setSecurityGroup(securityGroupConverter.convert(source.getSecurityGroup()));
        String instanceGroupName = source.getName();
        instanceGroup.setGroupName(instanceGroupName);
        instanceGroup.setInstanceGroupType(source.getType());
        instanceGroup.setAttributes(defaultInstanceGroupProvider.createAttributes(cloudPlatform, stackName, instanceGroupName));
        if (source.getNodeCount() > 0) {
            addInstanceMetadatas(source, instanceGroup);
        }
        instanceGroup.setNodeCount(source.getNodeCount());
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
