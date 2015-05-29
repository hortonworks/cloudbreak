package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_OPENSTACK_HEAT_TEMPLATE_PATH;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import jersey.repackaged.com.google.common.collect.Maps;

@Service
public class HeatTemplateBuilder {

    public static final String CB_INSTANCE_GROUP_NAME = "cb_instance_group_name";
    public static final String CB_INSTANCE_PRIVATE_ID = "cb_instance_private_id";

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatTemplateBuilder.class);

    private static final int PRIVATE_ID_PART = 2;
    private static final String MOUNT_PREFIX = "/hadoopfs/fs";
    private static final String DEVICE_PREFIX = "/dev/vd";
    private static final char[] DEVICE_CHAR = {'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k'};

    @Value("${cb.openstack.heat.template.path:" + CB_OPENSTACK_HEAT_TEMPLATE_PATH + "}")
    private String openStackHeatTemplatePath;

    @Inject
    private OpenStackUtil openStackUtil;

    @Inject
    private Configuration freemarkerConfiguration;

    public String build(Stack stack, String gateWayUserData, String coreUserData) {
        List<OpenStackInstance> agents = generateAgents(stack.getInstanceGroups());
        return build(stack, agents, gateWayUserData, coreUserData);
    }

    public String update(Stack stack, String gateWayUserData, String coreUserData, Set<InstanceMetaData> instanceMetadata) {
        List<OpenStackInstance> agents = regenerateAgents(instanceMetadata);
        return build(stack, agents, gateWayUserData, coreUserData);
    }

    public String remove(Stack stack, String gateWayUserData, String coreUserData, Set<InstanceMetaData> instanceMetadata,
            Set<String> removeInstances, String instanceGroup) {
        Set<Integer> privateIds = new HashSet<>();
        for (String instanceId : removeInstances) {
            privateIds.add(getPrivateId(instanceId));
        }
        List<OpenStackInstance> agents = generateAgentsWithFilter(instanceMetadata, privateIds, instanceGroup);
        return build(stack, agents, gateWayUserData, coreUserData);
    }

    public String add(Stack stack, String gateWayUserData, String coreUserData, Set<InstanceMetaData> instanceMetadata,
            String instanceGroup, int adjustment, InstanceGroup group) {
        List<OpenStackInstance> agents = generateNewAgents(instanceMetadata, instanceGroup, adjustment, group);
        return build(stack, agents, gateWayUserData, coreUserData);
    }

    private String build(Stack stack, List<OpenStackInstance> agents, String gateWayUserData, String coreUserData) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("cb_stack_name", stack.getName());
            model.put("agents", agents);
            model.put("core_user_data", formatUserData(coreUserData));
            model.put("gateway_user_data", formatUserData(gateWayUserData));
            model.put("subnets", stack.getAllowedSubnets());
            model.put("ports", NetworkUtils.getPorts(stack));
            String generatedTemplate = processTemplateIntoString(freemarkerConfiguration.getTemplate(openStackHeatTemplatePath, "UTF-8"), model);
            LOGGER.debug("Generated Heat template: {}",  generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new OpenStackResourceException("Failed to process the OpenStack HeatTemplate", e);
        }
    }

    public Map<String, String> buildParameters(String publicNetId, OpenStackCredential credential, String image, Network network) {
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("public_net_id", publicNetId);
        parameters.put("image_id", image);
        parameters.put("key_name", openStackUtil.getKeyPairName(credential));
        parameters.put("app_net_cidr", network.getSubnetCIDR());
        return parameters;
    }

    private List<OpenStackInstance> generateAgents(Set<InstanceGroup> instanceGroups) {
        List<OpenStackInstance> agents = new ArrayList<>();
        for (InstanceGroup group : instanceGroups) {
            String groupName = group.getGroupName();
            OpenStackTemplate template = (OpenStackTemplate) group.getTemplate();
            int privateId = 0;
            for (int i = 0; i < group.getNodeCount(); i++) {
                List<OpenStackVolume> volumes = buildVolumes(template.getVolumeCount(), template.getVolumeSize());
                Map<String, String> metadata = generateMetadata(groupName, privateId);
                OpenStackInstance instance = new OpenStackInstance(template.getInstanceType(), volumes, metadata, group.getInstanceGroupType());
                agents.add(instance);
                ++privateId;
            }
        }
        return agents;
    }

    private List<OpenStackInstance> regenerateAgents(Set<InstanceMetaData> instanceMetadata) {
        return generateAgentsWithFilter(instanceMetadata, new HashSet<Integer>(), null);
    }

    private List<OpenStackInstance> generateAgentsWithFilter(Set<InstanceMetaData> instanceMetadata, Set<Integer> idFilter, String groupFilter) {
        List<OpenStackInstance> agents = new ArrayList<>();
        for (InstanceMetaData data : instanceMetadata) {
            InstanceGroup group = data.getInstanceGroup();
            String groupName = group.getGroupName();
            int privateId = getPrivateId(data.getInstanceId());
            if (groupName.equals(groupFilter) && idFilter.contains(privateId)) {
                continue;
            }
            OpenStackTemplate template = (OpenStackTemplate) group.getTemplate();
            List<OpenStackVolume> volumes = buildVolumes(template.getVolumeCount(), template.getVolumeSize());
            Map<String, String> metadata = generateMetadata(groupName, privateId);
            OpenStackInstance agent = new OpenStackInstance(template.getInstanceType(), volumes, metadata, group.getInstanceGroupType());
            agents.add(agent);
        }
        return agents;
    }

    private List<OpenStackInstance> generateNewAgents(Set<InstanceMetaData> instanceMetadata, String instanceGroup, int adjustment, InstanceGroup group) {
        List<OpenStackInstance> agents = regenerateAgents(instanceMetadata);
        Set<Integer> existingIds = new HashSet<>();
        for (OpenStackInstance agent : agents) {
            Map<String, String> metadata = agent.getMetadataAsMap();
            if (instanceGroup.equals(metadata.get(CB_INSTANCE_GROUP_NAME))) {
                existingIds.add(Integer.valueOf(metadata.get(CB_INSTANCE_PRIVATE_ID)));
            }
        }
        OpenStackTemplate template = getTemplate(instanceMetadata, instanceGroup);
        while (adjustment > 0) {
            int privateId = 0;
            while (existingIds.contains(privateId)) {
                privateId++;
            }
            List<OpenStackVolume> volumes = buildVolumes(template.getVolumeCount(), template.getVolumeSize());
            Map<String, String> metadata = generateMetadata(instanceGroup, privateId);
            OpenStackInstance instance = new OpenStackInstance(template.getInstanceType(), volumes, metadata, group.getInstanceGroupType());
            agents.add(instance);
            adjustment--;
        }
        return agents;
    }

    private Map<String, String> generateMetadata(String groupName, int privateId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(CB_INSTANCE_GROUP_NAME, groupName);
        metadata.put(CB_INSTANCE_PRIVATE_ID, "" + privateId);
        return metadata;
    }

    private int getPrivateId(String instanceId) {
        return Integer.valueOf(instanceId.split("_")[PRIVATE_ID_PART]);
    }

    private OpenStackTemplate getTemplate(Set<InstanceMetaData> instanceMetaData, String instanceGroup) {
        for (InstanceMetaData metaData : instanceMetaData) {
            if (metaData.getInstanceGroup().getGroupName().equals(instanceGroup)) {
                return (OpenStackTemplate) metaData.getInstanceGroup().getTemplate();
            }
        }
        return null;
    }

    private List<OpenStackVolume> buildVolumes(int numDisk, int size) {
        List<OpenStackVolume> volumes = Lists.newArrayList();
        for (int i = 0; i < numDisk; i++) {
            volumes.add(new OpenStackVolume(MOUNT_PREFIX + (i + 1), DEVICE_PREFIX + DEVICE_CHAR[i], size));
        }
        return volumes;
    }

    private String formatUserData(String userData) {
        String[] lines = userData.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            // be aware of the OpenStack Heat template formatting
            sb.append("            " + lines[i] + "\n");
        }
        return sb.toString();
    }

}