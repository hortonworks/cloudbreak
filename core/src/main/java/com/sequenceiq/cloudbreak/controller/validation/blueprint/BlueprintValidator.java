package com.sequenceiq.cloudbreak.controller.validation.blueprint;

import static com.sequenceiq.cloudbreak.api.model.InstanceGroupType.GATEWAY;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

@Component
public class BlueprintValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintValidator.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    public void validateBlueprintForStack(Blueprint blueprint, Set<HostGroup> hostGroups, Set<InstanceGroup> instanceGroups) {
        try {
            JsonNode hostGroupsNode = getHostGroupNode(blueprint);
            validateHostGroups(hostGroupsNode, hostGroups, instanceGroups);
            Map<String, HostGroup> hostGroupMap = createHostGroupMap(hostGroups);
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap = Maps.newHashMap();
            for (JsonNode hostGroupNode : hostGroupsNode) {
                validateHostGroup(hostGroupNode, hostGroupMap, blueprintServiceComponentMap);
            }
            validateBlueprintServiceComponents(blueprintServiceComponentMap);
        } catch (IOException e) {
            throw new BadRequestException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.getId()));
        }
    }

    public JsonNode getHostGroupNode(Blueprint blueprint) throws IOException {
        JsonNode blueprintJsonTree = createJsonTree(blueprint);
        return blueprintJsonTree.get("host_groups");
    }

    private JsonNode createJsonTree(Blueprint blueprint) throws IOException {
        return objectMapper.readTree(blueprint.getBlueprintText());
    }

    private void validateHostGroups(JsonNode hostGroupsNode, Set<HostGroup> hostGroups, Set<InstanceGroup> instanceGroups) {
        Set<String> hostGroupsInRequest = getHostGroupsFromRequest(hostGroups);
        Set<String> hostGroupsInBlueprint = getHostGroupsFromBlueprint(hostGroupsNode);

        if (!hostGroupsInRequest.containsAll(hostGroupsInBlueprint) || !hostGroupsInBlueprint.containsAll(hostGroupsInRequest)) {
            throw new BadRequestException(String.format("The host groups in the blueprint must match the hostgroups in the request."));
        }

        if (!instanceGroups.isEmpty()) {
            Set<String> instanceGroupNames = new HashSet<>();
            for (HostGroup hostGroup : hostGroups) {
                String instanceGroupName = hostGroup.getConstraint().getInstanceGroup().getGroupName();
                if (instanceGroupNames.contains(instanceGroupName)) {
                    throw new BadRequestException(String.format(
                            "Instance group '%s' is assigned to more than one hostgroup.", instanceGroupName));
                }
                instanceGroupNames.add(instanceGroupName);
            }
            if (instanceGroups.size() - GATEWAY.getFixedNodeCount() != hostGroupsInRequest.size()) {
                throw new BadRequestException("The number of hostgroups must match the number of instance groups on the stack");
            }
        }
    }

    public void validateHostGroupScalingRequest(Blueprint blueprint, HostGroup hostGroup, Integer adjustment) {
        try {
            JsonNode hostGroupsNode = getHostGroupNode(blueprint);
            Map<String, HostGroup> hostGroupMap = createHostGroupMap(Collections.singleton(hostGroup));
            for (JsonNode hostGroupNode : hostGroupsNode) {
                //TODO
//                if (hostGroup.getName().equals(hostGroupNode.get("name").asText())) {
//                    InstanceGroup instanceGroup = hostGroup.getInstanceGroup();
//                    instanceGroup.setNodeCount(instanceGroup.getNodeCount() + adjustment);
//                    try {
//                        validateHostGroup(hostGroupNode, hostGroupMap, new HashMap<String, BlueprintServiceComponent>());
//                    } catch (BadRequestException be) {
//                        throw be;
//                    } finally {
//                        instanceGroup.setNodeCount(instanceGroup.getNodeCount() - adjustment);
//                    }
//                    break;
//                }
            }
        } catch (IOException e) {
            throw new BadRequestException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.getId()));
        }
    }

    private Set<String> getHostGroupsFromRequest(Set<HostGroup> hostGroup) {
        return FluentIterable.from(hostGroup).transform(new Function<HostGroup, String>() {
            @Nullable
            @Override
            public String apply(@Nullable HostGroup hostGroup) {
                return hostGroup.getName();
            }
        }).toSet();
    }

    private Set<String> getHostGroupsFromBlueprint(JsonNode hostGroupsNode) {
        Set<String> hostGroupsInBlueprint = new HashSet<>();
        Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
        while (hostGroups.hasNext()) {
            hostGroupsInBlueprint.add(hostGroups.next().get("name").asText());
        }
        return hostGroupsInBlueprint;
    }

    public Map<String, HostGroup> createHostGroupMap(Set<HostGroup> hostGroups) {
        Map<String, HostGroup> groupMap = Maps.newHashMap();
        for (HostGroup hostGroup : hostGroups) {
            groupMap.put(hostGroup.getName(), hostGroup);
        }
        return groupMap;
    }

    private void validateHostGroup(JsonNode hostGroupNode, Map<String, HostGroup> hostGroupMap,
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        String hostGroupName = getHostGroupName(hostGroupNode);
        HostGroup hostGroup = getHostGroup(hostGroupMap, hostGroupName);
        JsonNode componentsNode = getComponentsNode(hostGroupNode);
        for (JsonNode componentNode : componentsNode) {
            validateComponent(componentNode, hostGroup, blueprintServiceComponentMap);
        }
    }

    public String getHostGroupName(JsonNode hostGroupNode) {
        return hostGroupNode.get("name").asText();
    }

    public HostGroup getHostGroup(Map<String, HostGroup> hostGroupMap, String hostGroupName) {
        return hostGroupMap.get(hostGroupName);
    }

    public JsonNode getComponentsNode(JsonNode hostGroupNode) {
        return hostGroupNode.get("components");
    }

    private void validateComponent(JsonNode componentNode, HostGroup hostGroup, Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        String componentName = componentNode.get("name").asText();
        StackServiceComponentDescriptor componentDescriptor = stackServiceComponentDescs.get(componentName);
        if (componentDescriptor != null) {
            validateComponentCardinality(componentDescriptor, hostGroup);
            updateBlueprintServiceComponentMap(componentDescriptor, hostGroup, blueprintServiceComponentMap);
        }
    }

    private void validateComponentCardinality(StackServiceComponentDescriptor componentDescriptor, HostGroup hostGroup) {
        int nodeCount = hostGroup.getConstraint().getHostCount();
        int minCardinality = componentDescriptor.getMinCardinality();
        int maxCardinality = componentDescriptor.getMaxCardinality();
        if (componentDescriptor.isMaster() && !isNodeCountCorrect(nodeCount, minCardinality, maxCardinality)) {
            throw new BadRequestException(String.format(
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
                if (!isNodeCountCorrect(nodeCount, minCardinality, maxCardinality)) {
                    throw new BadRequestException(String.format("Incorrect number of '%s' components are in '%s' hostgroups: count: %d, min: %d max: %d",
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
            blueprintServiceComponent = new BlueprintServiceComponent(componentName, hostGroup.getName(), hostGroup.getConstraint().getHostCount());
            blueprintServiceComponentMap.put(componentName, blueprintServiceComponent);
        } else {
            blueprintServiceComponent.update(hostGroup);
        }
    }
}
