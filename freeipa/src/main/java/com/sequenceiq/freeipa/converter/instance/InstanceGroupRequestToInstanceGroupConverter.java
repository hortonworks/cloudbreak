package com.sequenceiq.freeipa.converter.instance;

import static com.sequenceiq.freeipa.util.CloudArgsForIgConverter.DISK_ENCRYPTION_SET_ID;
import static com.sequenceiq.freeipa.util.CloudArgsForIgConverter.GCP_KMS_ENCRYPTION_KEY;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.converter.instance.template.InstanceTemplateRequestToTemplateConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;
import com.sequenceiq.freeipa.util.CloudArgsForIgConverter;

@Component
public class InstanceGroupRequestToInstanceGroupConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupRequestToInstanceGroupConverter.class);

    @Inject
    private InstanceTemplateRequestToTemplateConverter templateConverter;

    @Inject
    private SecurityGroupRequestToSecurityGroupConverter securityGroupConverter;

    @Inject
    private DefaultInstanceGroupProvider defaultInstanceGroupProvider;

    public InstanceGroup convert(InstanceGroupRequest source, String accountId, String cloudPlatformString, String stackName, String hostname, String domain,
            EnumMap<CloudArgsForIgConverter, String> cloudArgsForIgConverter) {
        InstanceGroup instanceGroup = new InstanceGroup();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(cloudPlatformString);
        instanceGroup.setTemplate(source.getInstanceTemplate() == null
                ? defaultInstanceGroupProvider.createDefaultTemplate(cloudPlatform, accountId,
                    cloudArgsForIgConverter.get(DISK_ENCRYPTION_SET_ID), cloudArgsForIgConverter.get(GCP_KMS_ENCRYPTION_KEY))
                : templateConverter.convert(source.getInstanceTemplate(), cloudPlatform, accountId,
                    cloudArgsForIgConverter.get(DISK_ENCRYPTION_SET_ID), cloudArgsForIgConverter.get(GCP_KMS_ENCRYPTION_KEY)));
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
