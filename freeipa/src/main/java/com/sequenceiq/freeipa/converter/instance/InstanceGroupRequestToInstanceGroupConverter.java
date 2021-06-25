package com.sequenceiq.freeipa.converter.instance;

import java.util.LinkedHashSet;
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

    public InstanceGroup convert(InstanceGroupRequest source, String accountId, String cloudPlatformString, String stackName, String hostname, String domain,
            String diskEncryptionSetId) {
        InstanceGroup instanceGroup = new InstanceGroup();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(cloudPlatformString);
        instanceGroup.setTemplate(source.getInstanceTemplate() == null
                ? defaultInstanceGroupProvider.createDefaultTemplate(cloudPlatform, accountId, diskEncryptionSetId)
                : templateConverter.convert(source.getInstanceTemplate(), cloudPlatform, accountId, diskEncryptionSetId));
        instanceGroup.setSecurityGroup(securityGroupConverter.convert(source.getSecurityGroup()));
        String instanceGroupName = source.getName();
        instanceGroup.setGroupName(instanceGroupName);
        instanceGroup.setInstanceGroupType(source.getType());
        instanceGroup.setAttributes(defaultInstanceGroupProvider.createAttributes(cloudPlatform, stackName, instanceGroupName));
        if (source.getNodeCount() > 0) {
            addInstanceMetadatas(source, instanceGroup, hostname, domain);
        }
        instanceGroup.setNodeCount(source.getNodeCount());
        return instanceGroup;
    }

    private void addInstanceMetadatas(InstanceGroupRequest request, InstanceGroup instanceGroup, String hostname, String domain) {
        Set<InstanceMetaData> instanceMetaDataSet = new LinkedHashSet<>();
        for (int i = 0; i < request.getNodeCount(); i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(hostname + String.format("%d.", i) + domain);
            instanceMetaDataSet.add(instanceMetaData);
        }
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
    }
}
