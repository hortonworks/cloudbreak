package com.sequenceiq.cloudbreak.blueprint.validation;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Component
public class BlueprintValidator {

    private static final String KNOX = "KNOX_GATEWAY";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    public void validateBlueprintForStack(Blueprint blueprint, Set<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups)
            throws BlueprintValidationException {
        validateBlueprintForStack(null, blueprint, hostGroups, instanceGroups);
    }

    public void validateBlueprintForStack(Cluster cluster, Blueprint blueprint, Collection<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups) {
        try {
            JsonNode hostGroupsNode = getHostGroupNode(blueprint);
            validateHostGroups(hostGroupsNode, hostGroups, instanceGroups);
            Map<String, HostGroup> hostGroupMap = createHostGroupMap(hostGroups);
            Map<String, BlueprintServiceComponent> blueprintServiceComponentMap = Maps.newHashMap();
            for (JsonNode hostGroupNode : hostGroupsNode) {
                validateHostGroup(hostGroupNode, hostGroupMap, blueprintServiceComponentMap);
            }
            validateBlueprintServiceComponents(blueprintServiceComponentMap);
            validateKnoxWithKerberos(cluster, instanceGroups, blueprintServiceComponentMap);
        } catch (IOException e) {
            throw new BlueprintValidationException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.getId()), e);
        }
    }

    public JsonNode getHostGroupNode(Blueprint blueprint) throws IOException {
        JsonNode blueprintJsonTree = createJsonTree(blueprint);
        return blueprintJsonTree.get("host_groups");
    }

    private JsonNode createJsonTree(Blueprint blueprint) throws IOException {
        return objectMapper.readTree(blueprint.getBlueprintText());
    }

    private void validateHostGroups(JsonNode hostGroupsNode, Collection<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups) {
        Set<String> hostGroupsInRequest = getHostGroupsFromRequest(hostGroups);
        Set<String> hostGroupsInBlueprint = getHostGroupsFromBlueprint(hostGroupsNode);

        if (!hostGroupsInRequest.containsAll(hostGroupsInBlueprint) || !hostGroupsInBlueprint.containsAll(hostGroupsInRequest)) {
            throw new BlueprintValidationException("The host groups in the validation [" + String.join(",", hostGroupsInBlueprint) + "] "
                    + "must match the hostgroups in the request [" + String.join(",", hostGroupsInRequest) + "].");
        }

        if (!instanceGroups.isEmpty()) {
            Collection<String> instanceGroupNames = new HashSet<>();
            for (HostGroup hostGroup : hostGroups) {
                String instanceGroupName = hostGroup.getConstraint().getInstanceGroup().getGroupName();
                if (instanceGroupNames.contains(instanceGroupName)) {
                    throw new BlueprintValidationException(String.format(
                            "Instance group '%s' is assigned to more than one hostgroup.", instanceGroupName));
                }
                instanceGroupNames.add(instanceGroupName);
            }
            if (instanceGroups.size() < hostGroupsInRequest.size()) {
                throw new BlueprintValidationException("Each host group must have an instance group");
            }
        }
    }

    public void validateHostGroupScalingRequest(Blueprint blueprint, HostGroup hostGroup, Integer adjustment) {
        try {
            JsonNode hostGroupsNode = getHostGroupNode(blueprint);
            Map<String, HostGroup> hostGroupMap = createHostGroupMap(Collections.singleton(hostGroup));
            for (JsonNode hostGroupNode : hostGroupsNode) {
                if (hostGroup.getName().equals(hostGroupNode.get("name").asText())) {
                    hostGroup.getConstraint().setHostCount(hostGroup.getConstraint().getHostCount() + adjustment);
                    try {
                        validateHostGroup(hostGroupNode, hostGroupMap, new HashMap<>());
                    } finally {
                        hostGroup.getConstraint().setHostCount(hostGroup.getConstraint().getHostCount() - adjustment);
                    }
                    break;
                }
            }
        } catch (IOException ignored) {
            throw new BlueprintValidationException(String.format("Blueprint [%s] can not be parsed from JSON.", blueprint.getId()));
        }
    }

    private Set<String> getHostGroupsFromRequest(Collection<HostGroup> hostGroup) {
        return hostGroup.stream().map(HostGroup::getName).collect(Collectors.toSet());
    }

    private Set<String> getHostGroupsFromBlueprint(JsonNode hostGroupsNode) {
        Set<String> hostGroupsInBlueprint = new HashSet<>();
        Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
        while (hostGroups.hasNext()) {
            hostGroupsInBlueprint.add(hostGroups.next().get("name").asText());
        }
        return hostGroupsInBlueprint;
    }

    public Map<String, HostGroup> createHostGroupMap(Iterable<HostGroup> hostGroups) {
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
            blueprintServiceComponent = new BlueprintServiceComponent(componentName, hostGroup.getName(), hostGroup.getConstraint().getHostCount());
            blueprintServiceComponentMap.put(componentName, blueprintServiceComponent);
        } else {
            blueprintServiceComponent.update(hostGroup);
        }
    }

    private void validateKnoxWithKerberos(Cluster cluster, Collection<InstanceGroup> instanceGroups, Map<String, BlueprintServiceComponent> componentMap) {
        if (cluster != null && cluster.isSecure() && cluster.getGateway() != null && cluster.getGateway().getEnableGateway()) {
            List<String> missingNodes = instanceGroups.stream()
                .filter(s -> {
                    if (!s.getInstanceGroupType().equals(InstanceGroupType.GATEWAY)) {
                        return false;
                    }
                    return componentMap.values().stream()
                        .filter(c -> c.getHostgroups().contains(s.getGroupName()))
                        .noneMatch(c -> c.getName().equals(KNOX));
                })
                .map(InstanceGroup::getGroupName)
                .collect(Collectors.toList());
            if (!missingNodes.isEmpty()) {
                Collections.sort(missingNodes);
                throw new BlueprintValidationException("In case of Knox and Kerberos each 'Ambari Server' node must include the 'KNOX_GATEWAY' service. "
                    + "The following host groups are missing the service: " + String.join(",", missingNodes));
            }
        }
    }
}
