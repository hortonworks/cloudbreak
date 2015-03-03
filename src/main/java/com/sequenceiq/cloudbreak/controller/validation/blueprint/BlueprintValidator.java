package com.sequenceiq.cloudbreak.controller.validation.blueprint;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    public void validateBlueprintForStack(Blueprint blueprint, Set<HostGroup> hostGroups, Set<InstanceGroup> instanceGroups) {
        try {
            JsonNode blueprintJsonTree = createJsonTree(blueprint);
            JsonNode hostGroupsNode = blueprintJsonTree.get("host_groups");
            validateHostGroups(hostGroupsNode, hostGroups, instanceGroups);
            Map<String, HostGroup> hostGroupMap = createHostGroupMap(hostGroups);
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap = Maps.newHashMap();
            for (JsonNode hostGroupNode : hostGroupsNode) {
                validateHostGroup(hostGroupNode, hostGroupMap, blueprintServiceComponentMap);
            }
            validateBlueprintServiceComponents(blueprintServiceComponentMap);
        }  catch (IOException e) {
            throw new BadRequestException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.getId()));
        }
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

        Set<Long> instanceGroupNames = new HashSet<>();
        for (HostGroup hostGroup : hostGroups) {
            if (instanceGroupNames.contains(hostGroup.getInstanceGroup().getGroupName())) {
                throw new BadRequestException(String.format(
                        "Instance group '%s' is assigned to more than one hostgroup.", hostGroup.getInstanceGroup().getGroupName()));
            }
            instanceGroupNames.add(hostGroup.getInstanceGroup().getId());
        }

        if (instanceGroups.size() != hostGroupsInRequest.size()) {
            throw new BadRequestException("The number of hostgroups must match the number of instance groups on the stack");
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

    private Map<String, HostGroup> createHostGroupMap(Set<HostGroup> hostGroups) {
        Map<String, HostGroup> groupMap = Maps.newHashMap();
        for (HostGroup hostGroup : hostGroups) {
            groupMap.put(hostGroup.getName(), hostGroup);
        }
        return groupMap;
    }

    private void validateHostGroup(JsonNode hostGroupNode, Map<String, HostGroup> hostGroupMap,
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        String hostGroupName = hostGroupNode.get("name").asText();
        HostGroup hostGroup = hostGroupMap.get(hostGroupName);
        JsonNode componentsNode = hostGroupNode.get("components");
        for (JsonNode componentNode : componentsNode) {
            validateComponent(componentNode, hostGroup, blueprintServiceComponentMap);
        }
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
        int nodeCount = hostGroup.getInstanceGroup().getNodeCount();
        int maxCardinality = componentDescriptor.getMaxCardinality();
        if (componentDescriptor.isMaster() && nodeCount > maxCardinality) {
            throw new BadRequestException(String.format("The nodecount '%d' for hostgroup '%s' cannot be more than '%d' because of '%s' component",
                    nodeCount, hostGroup.getName(), maxCardinality, componentDescriptor.getName()));
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

    private void updateBlueprintServiceComponentMap(StackServiceComponentDescriptor componentDescriptor, HostGroup hostGroup,
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap) {
        String componentName = componentDescriptor.getName();
        BlueprintServiceComponent blueprintServiceComponent = blueprintServiceComponentMap.get(componentName);
        if (blueprintServiceComponent == null) {
            blueprintServiceComponent = new BlueprintServiceComponent(componentName, hostGroup.getName(), hostGroup.getInstanceGroup().getNodeCount());
            blueprintServiceComponentMap.put(componentName, blueprintServiceComponent);
        } else {
            blueprintServiceComponent.update(hostGroup);
        }
    }
}
