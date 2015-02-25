package com.sequenceiq.cloudbreak.controller.validation.blueprint;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

@Component
public class BlueprintValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintValidator.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    public void validateBlueprintForStack(Blueprint blueprint, Set<InstanceGroup> instanceGroups) {
        try {
            JsonNode blueprintJsonTree = createJsonTree(blueprint);
            Map<String, InstanceGroup> instanceGroupMap = createInstanceGroupMap(instanceGroups);
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap = Maps.newHashMap();
            JsonNode hostGroupsNode = blueprintJsonTree.get("host_groups");
            int hostGroupCount = 0;
            for (JsonNode hostGroupNode : hostGroupsNode) {
                validateHostGroup(hostGroupNode, instanceGroupMap, blueprintServiceComponentMap);
                hostGroupCount++;
            }
            validateHostGroupCount(hostGroupCount, instanceGroupMap.size());
            validateBlueprintServiceComponents(blueprintServiceComponentMap);
        } catch (IOException iox) {
            LOGGER.error("Cannot parse blueprint json", iox);
            throw new BadRequestException(String.format("Blueprint [%s] can not convert to json tree.", blueprint.getId()));
        }
    }

    private JsonNode createJsonTree(Blueprint blueprint) throws IOException {
        return objectMapper.readTree(blueprint.getBlueprintText());
    }

    private Map<String, InstanceGroup> createInstanceGroupMap(Set<InstanceGroup> instanceGroups) {
        Map<String, InstanceGroup> groupMap = Maps.newHashMap();
        for (InstanceGroup instanceGroup : instanceGroups) {
            groupMap.put(instanceGroup.getGroupName(), instanceGroup);
        }
        return groupMap;
    }

    private void validateHostGroup(JsonNode hostGroupNode, Map<String, InstanceGroup> instanceGroupMap,
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        String hostGroupName = hostGroupNode.get("name").asText();
        validateHostGroupName(hostGroupName, instanceGroupMap.keySet());
        InstanceGroup instanceGroup = instanceGroupMap.get(hostGroupName);
        JsonNode componentsNode = hostGroupNode.get("components");
        for (JsonNode componentNode : componentsNode) {
            validateComponent(componentNode, instanceGroup, blueprintServiceComponentMap);
        }
    }

    private void validateComponent(JsonNode componentNode, InstanceGroup instanceGroup, Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        String componentName = componentNode.get("name").asText();
        StackServiceComponentDescriptor componentDescriptor = stackServiceComponentDescs.get(componentName);
        if (componentDescriptor != null) {
            validateComponentCardinality(componentDescriptor, instanceGroup);
            updateBlueprintServiceComponentMap(componentDescriptor, instanceGroup, blueprintServiceComponentMap);
        }
    }

    private void validateComponentCardinality(StackServiceComponentDescriptor componentDescriptor, InstanceGroup instanceGroup) {
        int nodeCount = instanceGroup.getNodeCount();
        int maxCardinality = componentDescriptor.getMaxCardinality();
        if (componentDescriptor.isMaster() && nodeCount > maxCardinality) {
            throw new BadRequestException(String.format("The nodecount '%d' for hostgroup '%s' cannot be more than '%d' because of '%s' component",
                    nodeCount, instanceGroup.getGroupName(), maxCardinality, componentDescriptor.getName()));
        }
    }

    private void validateHostGroupName(String groupName, Set<String> instanceGroupNames) {
        if (!instanceGroupNames.contains(groupName)) {
            throw new BadRequestException(String.format("The name of host group '%s' doesn't match any of the defined instance groups.", groupName));
        }
    }

    private void validateHostGroupCount(int hostGroupCount, int instanceGroupCount) {
        if (hostGroupCount != instanceGroupCount) {
            throw new BadRequestException(String.format("The number of instancegroups and hostgroups must be equals."));
        }
    }

    private void validateBlueprintServiceComponents(Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        for (BlueprintServiceComponent blueprintServiceComponent : blueprintServiceComponentMap.values()) {
            String componentName = blueprintServiceComponent.getName();
            int nodeCount = blueprintServiceComponent.getNodeCount();
            StackServiceComponentDescriptor stackServiceComponentDescriptor = stackServiceComponentDescs.get(componentName);
            int maxCardinality = stackServiceComponentDescriptor.getMaxCardinality();
            if (stackServiceComponentDescriptor != null && nodeCount > maxCardinality) {
                throw new BadRequestException(String.format("Too much '%s' components are in '%s' hostgroups: count: %d, max: %d",
                        componentName, blueprintServiceComponent.getHostgroups().toString(), nodeCount, maxCardinality));
            }
        }
    }

    private void updateBlueprintServiceComponentMap(StackServiceComponentDescriptor componentDescriptor, InstanceGroup instanceGroup,
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        String componentName = componentDescriptor.getName();
        BlueprintServiceComponent blueprintServiceComponent = blueprintServiceComponentMap.get(componentName);
        if (blueprintServiceComponent == null) {
            blueprintServiceComponent = new BlueprintServiceComponent(componentName, instanceGroup.getGroupName(), instanceGroup.getNodeCount());
            blueprintServiceComponentMap.put(componentName, blueprintServiceComponent);
        } else {
            blueprintServiceComponent.update(instanceGroup);
        }
    }
}
