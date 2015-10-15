package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.service.stack.flow.ReflectionUtils.getDeclaredFields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils;
import com.sequenceiq.cloudbreak.service.stack.flow.ReflectionUtils;

@Component
public class StackToCloudStackConverter {

    @Inject
    private SecurityRuleRepository securityRuleRepository;

    @Inject
    private ImageService imageService;

    public CloudStack convert(Stack stack) {
        return convert(stack, Collections.<String>emptySet());
    }

    public CloudStack convert(Stack stack, Set<String> deleteRequestedInstances) {
        List<Group> instanceGroups = buildInstanceGroups(stack.getInstanceGroupsAsList(), deleteRequestedInstances);
        Image image = imageService.getImage(stack.getId());
        Network network = buildNetwork(stack);
        Security security = buildSecurity(stack);
        return new CloudStack(instanceGroups, network, security, image, stack.getParameters());
    }

    public List<Group> buildInstanceGroups(List<InstanceGroup> instanceGroups, Set<String> deleteRequests) {
        // sort by name to avoid shuffling the different instance groups
        Collections.sort(instanceGroups);
        List<Group> groups = new ArrayList<>();
        long privateId = getFirstValidPrivateId(instanceGroups);
        for (InstanceGroup instanceGroup : instanceGroups) {
            List<InstanceTemplate> instances = new ArrayList<>();
            Template template = instanceGroup.getTemplate();
            int desiredNodeCount = instanceGroup.getNodeCount();
            // existing instances
            for (InstanceMetaData metaData : instanceGroup.getInstanceMetaData()) {
                InstanceStatus status = getInstanceStatus(metaData, deleteRequests);
                instances.add(buildInstanceTemplate(template, instanceGroup.getGroupName(), metaData.getPrivateId(), status));
            }
            // new instances
            int existingNodesSize = instances.size();
            if (existingNodesSize < desiredNodeCount) {
                for (long i = 0; i < desiredNodeCount - existingNodesSize; i++) {
                    instances.add(buildInstanceTemplate(template, instanceGroup.getGroupName(), privateId++, InstanceStatus.CREATE_REQUESTED));
                }
            }
            groups.add(new Group(instanceGroup.getGroupName(), instanceGroup.getInstanceGroupType(), instances));
        }
        return groups;
    }

    public List<InstanceTemplate> buildInstanceTemplates(Stack stack) {
        List<Group> groups = buildInstanceGroups(stack.getInstanceGroupsAsList(), Collections.<String>emptySet());
        List<InstanceTemplate> instanceTemplates = new ArrayList<>();
        for (Group group : groups) {
            instanceTemplates.addAll(group.getInstances());
        }
        return instanceTemplates;
    }

    public InstanceTemplate buildInstanceTemplate(Template template, String name, Long privateId, InstanceStatus status) {
        Map<String, Object> fields = getDeclaredFields(template);
        List<Volume> volumes = new ArrayList<>();
        for (int i = 0; i < template.getVolumeCount(); i++) {
            Volume volume = new Volume(VolumeUtils.VOLUME_PREFIX + (i + 1), template.getVolumeTypeName(), template.getVolumeSize());
            volumes.add(volume);
        }
        return new InstanceTemplate(template.getInstanceTypeName(), name, privateId, volumes, status, fields);
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

    private Long getFirstValidPrivateId(List<InstanceGroup> instanceGroups) {
        long highest = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            for (InstanceMetaData metaData : instanceGroup.getInstanceMetaData()) {
                Long privateId = metaData.getPrivateId();
                if (privateId == null) {
                    continue;
                }
                if (privateId > highest) {
                    highest = privateId;
                }
            }
        }
        return highest == 0 ? 0 : highest + 1;
    }

    private InstanceStatus getInstanceStatus(InstanceMetaData metaData, Set<String> deleteRequests) {
        return deleteRequests.contains(metaData.getInstanceId()) ? InstanceStatus.DELETE_REQUESTED
                : metaData.getInstanceStatus() == com.sequenceiq.cloudbreak.common.type.InstanceStatus.REQUESTED ? InstanceStatus.CREATE_REQUESTED
                : InstanceStatus.CREATED;
    }

}
