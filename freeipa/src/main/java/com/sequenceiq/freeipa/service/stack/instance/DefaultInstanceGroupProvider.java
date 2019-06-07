package com.sequenceiq.freeipa.service.stack.instance;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.freeipa.api.model.ResourceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;

@Service
public class DefaultInstanceGroupProvider {

    private static final String MASTER = "master";

    private static final int MASTER_NODE_COUNT = 1;

    private static final String TCP = "tcp";

    @Value("${freeipa.security.default.ports:22,443,9443}")
    private String defaultSecurityGroupPorts;

    @Value("${freeipa.security.default.cidr:0.0.0.0/0}")
    private String defaultSecurityGroupCidr;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    public Set<InstanceGroup> createDefaultInstanceGroups(String cloudPlatformString) {
        InstanceGroup instanceGroup = new InstanceGroup();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(cloudPlatformString);
        instanceGroup.setGroupName(MASTER);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        instanceGroup.setInstanceMetaData(createInstanceMetadatas(instanceGroup, MASTER_NODE_COUNT));
        instanceGroup.setTemplate(createDefaultTemplate(cloudPlatform));
        instanceGroup.setSecurityGroup(createDefaultSecurityGroup());
        return Set.of(instanceGroup);
    }

    private SecurityGroup createDefaultSecurityGroup() {
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setName(missingResourceNameGenerator.generateName(APIResourceType.SECURITY_GROUP));
        securityGroup.setSecurityRules(createDefaultSecurityRule(securityGroup));
        return securityGroup;
    }

    private Set<SecurityRule> createDefaultSecurityRule(SecurityGroup securityGroup) {
        SecurityRule securityRule = new SecurityRule();
        securityRule.setCidr(defaultSecurityGroupCidr);
        securityRule.setPorts(defaultSecurityGroupPorts);
        securityRule.setModifiable(false);
        securityRule.setProtocol(TCP);
        securityRule.setSecurityGroup(securityGroup);
        return Set.of(securityRule);
    }

    private Set<InstanceMetaData> createInstanceMetadatas(InstanceGroup instanceGroup, int nodeCount) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < nodeCount; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaDataSet.add(instanceMetaData);
        }
        return instanceMetaDataSet;
    }

    private Template createDefaultTemplate(CloudPlatform cloudPlatform) {
        Template template = new Template();
        template.setName(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        template.setStatus(ResourceStatus.DEFAULT);
        template.setRootVolumeSize(defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform.name()));
        template.setVolumeCount(0);
        template.setVolumeSize(0);
        template.setInstanceType(defaultInstanceTypeProvider.getForPlatform(cloudPlatform.name()));
        return template;
    }
}
