package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.network.NetworkConfig;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Service
public class HeatTemplateBuilder {

    public static final String CB_INSTANCE_GROUP_NAME = "cb_instance_group_name";
    public static final String CB_INSTANCE_PRIVATE_ID = "cb_instance_private_id";
    private static final int PRIVATE_ID_PART = 2;
    private static final String MOUNT_PREFIX = "/mnt/fs";
    private static final String DEVICE_PREFIX = "/dev/vd";
    private static final char[] DEVICE_CHAR = {'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k'};

    @Autowired
    private Configuration freemarkerConfiguration;

    public String build(Stack stack, String templatePath, String userData) {
        List<OpenStackInstance> agents = generateAgents(stack.getInstanceGroups());
        return build(stack, templatePath, agents, userData);
    }

    public String update(Stack stack, String templatePath, String userData, Set<InstanceMetaData> instanceMetadata) {
        List<OpenStackInstance> agents = regenerateAgents(instanceMetadata);
        return build(stack, templatePath, agents, userData);
    }

    public String remove(Stack stack, String templatePath, String userData, Set<InstanceMetaData> instanceMetadata,
            Set<String> removeInstances, String hostGroup) {
        Set<Integer> privateIds = new HashSet<>();
        for (String instanceId : removeInstances) {
            privateIds.add(getPrivateId(instanceId));
        }
        List<OpenStackInstance> agents = generateAgentsWithFilter(instanceMetadata, privateIds, hostGroup);
        return build(stack, templatePath, agents, userData);
    }

    public String add(Stack stack, String templatePath, String userData, Set<InstanceMetaData> instanceMetadata, String hostGroup, int adjustment) {
        List<OpenStackInstance> agents = generateNewAgents(instanceMetadata, hostGroup, adjustment);
        return build(stack, templatePath, agents, userData);
    }

    private String build(Stack stack, String templatePath, List<OpenStackInstance> agents, String userData) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("agents", agents);
            model.put("userdata", formatUserData(userData));
            model.put("subnets", stack.getAllowedSubnets());
            model.put("ports", NetworkUtils.getPorts(stack));
            model.put("cbSubnet", NetworkConfig.SUBNET_16);
            model.put("startIP", NetworkConfig.START_IP);
            model.put("endIP", NetworkConfig.END_IP);
            model.put("gatewayIP", NetworkConfig.GATEWAY_IP);
            return processTemplateIntoString(freemarkerConfiguration.getTemplate(templatePath, "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            throw new InternalServerException("Failed to process the OpenStack HeatTemplate", e);
        }
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
                OpenStackInstance instance = new OpenStackInstance(template.getInstanceType(), volumes, metadata);
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
            OpenStackInstance agent = new OpenStackInstance(template.getInstanceType(), volumes, metadata);
            agents.add(agent);
        }
        return agents;
    }

    private List<OpenStackInstance> generateNewAgents(Set<InstanceMetaData> instanceMetadata, String hostGroup, int adjustment) {
        List<OpenStackInstance> agents = regenerateAgents(instanceMetadata);
        Set<Integer> existingIds = new HashSet<>();
        for (OpenStackInstance agent : agents) {
            Map<String, String> metadata = agent.getMetadataAsMap();
            if (hostGroup.equals(metadata.get(CB_INSTANCE_GROUP_NAME))) {
                existingIds.add(Integer.valueOf(metadata.get(CB_INSTANCE_PRIVATE_ID)));
            }
        }
        OpenStackTemplate template = getTemplate(instanceMetadata, hostGroup);
        while (adjustment > 0) {
            int privateId = 0;
            while (existingIds.contains(privateId)) {
                privateId++;
            }
            List<OpenStackVolume> volumes = buildVolumes(template.getVolumeCount(), template.getVolumeSize());
            Map<String, String> metadata = generateMetadata(hostGroup, privateId);
            OpenStackInstance instance = new OpenStackInstance(template.getInstanceType(), volumes, metadata);
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

    private OpenStackTemplate getTemplate(Set<InstanceMetaData> instanceMetaData, String hostGroup) {
        for (InstanceMetaData metaData : instanceMetaData) {
            if (metaData.getInstanceGroup().getGroupName().equals(hostGroup)) {
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