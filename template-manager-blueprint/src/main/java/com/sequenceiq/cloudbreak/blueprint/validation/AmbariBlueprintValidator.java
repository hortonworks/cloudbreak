package com.sequenceiq.cloudbreak.blueprint.validation;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.template.validation.BlueprintServiceComponent;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidationException;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidatorUtil;

@Component
public class AmbariBlueprintValidator implements BlueprintValidator {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    @Override
    public void validate(Blueprint blueprint, Set<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups,
        boolean validateServiceCardinality) {
        try {
            JsonNode hostGroupsNode = getHostGroupNode(blueprint);
            BlueprintValidatorUtil.validateHostGroupsMatch(hostGroups, getHostGroupsFromBlueprint(hostGroupsNode));
            BlueprintValidatorUtil.validateInstanceGroups(hostGroups, instanceGroups);
            if (validateServiceCardinality) {
                Map<String, HostGroup> hostGroupMap = BlueprintValidatorUtil.createHostGroupMap(hostGroups);
                Map<String, BlueprintServiceComponent> blueprintServiceComponentMap = Maps.newHashMap();
                for (JsonNode hostGroupNode : hostGroupsNode) {
                    validateHostGroup(hostGroupNode, hostGroupMap, blueprintServiceComponentMap);
                }
                validateBlueprintServiceComponents(blueprintServiceComponentMap);
            }
        } catch (IOException e) {
            throw new BlueprintValidationException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.getId()), e);
        }
    }

    private JsonNode getHostGroupNode(Blueprint blueprint) throws IOException {
        JsonNode blueprintJsonTree = createJsonTree(blueprint);
        return blueprintJsonTree.get("host_groups");
    }

    private JsonNode createJsonTree(Blueprint blueprint) throws IOException {
        String blueprintText = blueprint.getBlueprintText();
        return objectMapper.readTree(blueprintText);
    }

    @Override
    public void validateHostGroupScalingRequest(Blueprint blueprint, HostGroup hostGroup, Integer adjustment) {
        try {
            JsonNode hostGroupsNode = getHostGroupNode(blueprint);
            Map<String, HostGroup> hostGroupMap = BlueprintValidatorUtil.createHostGroupMap(Collections.singleton(hostGroup));
            for (JsonNode hostGroupNode : hostGroupsNode) {
                if (hostGroup.getName().equals(hostGroupNode.get("name").asText())) {
                    validateHostGroup(hostGroupNode, hostGroupMap, new HashMap<>(), adjustment);
                    break;
                }
            }
        } catch (IOException ignored) {
            throw new BlueprintValidationException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.getId()));
        }
    }

    private Set<String> getHostGroupsFromBlueprint(JsonNode hostGroupsNode) {
        Set<String> hostGroupsInBlueprint = new HashSet<>();
        Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
        while (hostGroups.hasNext()) {
            hostGroupsInBlueprint.add(hostGroups.next().get("name").asText());
        }
        return hostGroupsInBlueprint;
    }

    private void validateHostGroup(JsonNode hostGroupNode, Map<String, HostGroup> hostGroupMap,
        Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        validateHostGroup(hostGroupNode, hostGroupMap, blueprintServiceComponentMap, 0);
    }

    private void validateHostGroup(JsonNode hostGroupNode, Map<String, HostGroup> hostGroupMap,
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap, int adjustment) {
        String hostGroupName = getHostGroupName(hostGroupNode);
        HostGroup hostGroup = getHostGroup(hostGroupMap, hostGroupName);
        JsonNode componentsNode = getComponentsNode(hostGroupNode);
        for (JsonNode componentNode : componentsNode) {
            validateComponent(componentNode, hostGroup, blueprintServiceComponentMap, adjustment);
        }
    }

    public String getHostGroupName(JsonNode hostGroupNode) {
        return hostGroupNode.get("name").asText();
    }

    private HostGroup getHostGroup(Map<String, HostGroup> hostGroupMap, String hostGroupName) {
        return hostGroupMap.get(hostGroupName);
    }

    private JsonNode getComponentsNode(JsonNode hostGroupNode) {
        return hostGroupNode.get("components");
    }

    private void validateComponent(JsonNode componentNode, HostGroup hostGroup, Map<String, BlueprintServiceComponent> blueprintServiceComponentMap,
        int adjusment) {
        String componentName = componentNode.get("name").asText();
        StackServiceComponentDescriptor componentDescriptor = stackServiceComponentDescs.get(componentName);
        if (componentDescriptor != null) {
            validateComponentCardinality(componentDescriptor, hostGroup, adjusment);
            updateBlueprintServiceComponentMap(componentDescriptor, hostGroup, blueprintServiceComponentMap);
        }
    }

    private void validateComponentCardinality(StackServiceComponentDescriptor componentDescriptor, HostGroup hostGroup, int adjustment) {
        int nodeCount = hostGroup.getInstanceGroup().getNodeCount() + adjustment;
        int minCardinality = componentDescriptor.getMinCardinality();
        int maxCardinality = componentDescriptor.getMaxCardinality();
        if (componentDescriptor.isMaster() && !isNodeCountCorrect(nodeCount, minCardinality, maxCardinality)) {
            throw new BlueprintValidationException(String.format(
                    "The node count '%d' for hostgroup '%s' cannot be less than '%d' or more than '%d' because of '%s' component", nodeCount,
                    hostGroup.getName(), minCardinality, maxCardinality, componentDescriptor.getName()));
        }
    }

    private void validateBlueprintServiceComponents(Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        for (BlueprintServiceComponent blueprintServiceComponent : blueprintServiceComponentMap.values()) {
            String componentName = blueprintServiceComponent.getName();
            int nodeCount = blueprintServiceComponent.getNodeCount();
            StackServiceComponentDescriptor stackServiceComponentDescriptor = stackServiceComponentDescs.get(componentName);
            if (stackServiceComponentDescriptor != null) {
                int minCardinality = stackServiceComponentDescriptor.getMinCardinality();
                int maxCardinality = stackServiceComponentDescriptor.getMaxCardinality();
                if (!isNodeCountCorrect(nodeCount, minCardinality, maxCardinality) && !(nodeCount == 0 && !stackServiceComponentDescriptor.isMaster())) {
                    throw new BlueprintValidationException(
                            String.format("Incorrect number of '%s' components are in '%s' hostgroups: count: %d, min: %d max: %d",
                                    componentName, blueprintServiceComponent.getHostgroups().toString(), nodeCount, minCardinality, maxCardinality));
                }
            }
        }
    }

    private boolean isNodeCountCorrect(int nodeCount, int minCardinality, int maxCardinality) {
        return minCardinality <= nodeCount && nodeCount <= maxCardinality;
    }

    private void updateBlueprintServiceComponentMap(StackServiceComponentDescriptor componentDescriptor, HostGroup hostGroup,
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        String componentName = componentDescriptor.getName();
        BlueprintServiceComponent blueprintServiceComponent = blueprintServiceComponentMap.get(componentName);
        if (blueprintServiceComponent == null) {
            blueprintServiceComponent = new BlueprintServiceComponent(componentName, hostGroup.getName(),
                    hostGroup.getInstanceGroup().getNodeCount());
            blueprintServiceComponentMap.put(componentName, blueprintServiceComponent);
        } else {
            blueprintServiceComponent.update(hostGroup);
        }
    }
}
