package com.sequenceiq.cloudbreak.converter.spi;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils;
import com.sequenceiq.cloudbreak.service.stack.flow.ReflectionUtils;

@Component
public class StackToCloudStackConverter {

    @Inject
    private SecurityRuleRepository securityRuleRepository;

    public CloudStack convert(Stack stack) {
        return convert(stack, null, null);
    }

    public CloudStack convert(Stack stack, String coreUserData, String gateWayUserData) {
        List<Group> instanceGroups = buildInstanceGroups(stack);
        Image image = buildImage(stack, coreUserData, gateWayUserData);
        Network network = buildNetwork(stack);
        Security security = buildSecurity(stack);
        return new CloudStack(instanceGroups, network, security, image);
    }

    private Image buildImage(Stack stack, String coreUserData, String gateWayUserData) {
        Image image = new Image(stack.getImage());
        image.putUserData(InstanceGroupType.CORE, coreUserData);
        image.putUserData(InstanceGroupType.GATEWAY, gateWayUserData);
        return image;
    }

    private Network buildNetwork(Stack stack) {
        com.sequenceiq.cloudbreak.domain.Network stackNetwork = stack.getNetwork();
        Subnet subnet = new Subnet(stackNetwork.getSubnetCIDR());
        Network network = new Network(subnet);
        network.putAll(ReflectionUtils.getDeclaredFields(stackNetwork));
        return network;
    }

    private Security buildSecurity(Stack stack) {
        Long id = stack.getSecurityGroup().getId();
        List<com.sequenceiq.cloudbreak.domain.SecurityRule> securityRules = securityRuleRepository.findAllBySecurityGroupId(id);
        List<SecurityRule> rules = new ArrayList<>();
        for (com.sequenceiq.cloudbreak.domain.SecurityRule securityRule : securityRules) {
            rules.add(new SecurityRule(securityRule.getCidr(), securityRule.getPorts(), securityRule.getProtocol()));
        }
        return new Security(rules);
    }

    public List<Group> buildInstanceGroups(Stack stack) {
        List<Group> groups = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Group group = new Group(instanceGroup.getGroupName(), instanceGroup.getInstanceGroupType());
            // FIXME nodeId shall be an "internal id" from Cloudbreak and not a counter
            for (int nodeId = 0; nodeId < instanceGroup.getNodeCount(); nodeId++) {
                Template template = instanceGroup.getTemplate();
                InstanceTemplate instance = new InstanceTemplate(template.getInstanceTypeName(), group.getName(), nodeId);
                for (int i = 0; i < template.getVolumeCount(); i++) {
                    Volume volume = new Volume(VolumeUtils.VOLUME_PREFIX + (i + 1), template.getVolumeTypeName(), template.getVolumeSize());
                    instance.addVolume(volume);
                }
                group.addInstance(instance);
            }
            groups.add(group);
        }
        return groups;
    }


    public List<InstanceTemplate> buildInstanceTemplates(Stack stack) {
        List<Group> groups = buildInstanceGroups(stack);
        List<InstanceTemplate> instanceTemplates = new ArrayList<>();
        for (Group group : groups) {
            instanceTemplates.addAll(group.getInstances());
        }
        return instanceTemplates;
    }

}
