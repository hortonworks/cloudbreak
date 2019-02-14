package com.sequenceiq.cloudbreak.clusterdefinition.validation;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.template.validation.ClusterDefinitionServiceComponent;
import com.sequenceiq.cloudbreak.template.validation.ClusterDefinitionValidationException;

@Component
public class AmbariBlueprintValidator {

    private static final String KNOX = "KNOX_GATEWAY";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    public void validateBlueprintForStack(ClusterDefinition clusterDefinition, Set<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups)
            throws ClusterDefinitionValidationException {
        validateBlueprintForStack(null, clusterDefinition, hostGroups, instanceGroups);
    }

    public void validateBlueprintForStack(Cluster cluster, ClusterDefinition clusterDefinition, Collection<HostGroup> hostGroups,
            Collection<InstanceGroup> instanceGroups) {
        try {
            JsonNode hostGroupsNode = getHostGroupNode(clusterDefinition);
            validateHostGroups(hostGroupsNode, hostGroups, instanceGroups);
            Map<String, HostGroup> hostGroupMap = createHostGroupMap(hostGroups);
            Map<String, ClusterDefinitionServiceComponent> blueprintServiceComponentMap = Maps.newHashMap();
            for (JsonNode hostGroupNode : hostGroupsNode) {
                validateHostGroup(hostGroupNode, hostGroupMap, blueprintServiceComponentMap);
            }
            validateBlueprintServiceComponents(blueprintServiceComponentMap);
        } catch (IOException e) {
            throw new ClusterDefinitionValidationException(String.format("Blueprint [%s] can not be parsed from JSON.", clusterDefinition.getId()), e);
        }
    }

    public JsonNode getHostGroupNode(ClusterDefinition clusterDefinition) throws IOException {
        JsonNode blueprintJsonTree = createJsonTree(clusterDefinition);
        return blueprintJsonTree.get("host_groups");
    }

    private JsonNode createJsonTree(ClusterDefinition clusterDefinition) throws IOException {
        String clusterDefinitionText = clusterDefinition.getClusterDefinitionText();
        return objectMapper.readTree(clusterDefinitionText);
    }

    private void validateHostGroups(JsonNode hostGroupsNode, Collection<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups) {
        Set<String> hostGroupsInRequest = getHostGroupsFromRequest(hostGroups);
        Set<String> hostGroupsInBlueprint = getHostGroupsFromBlueprint(hostGroupsNode);

        if (!hostGroupsInRequest.containsAll(hostGroupsInBlueprint) || !hostGroupsInBlueprint.containsAll(hostGroupsInRequest)) {
            throw new ClusterDefinitionValidationException("The host groups in the validation [" + String.join(",", hostGroupsInBlueprint) + "] "
                    + "must match the hostgroups in the request [" + String.join(",", hostGroupsInRequest) + "].");
        }

        if (!instanceGroups.isEmpty()) {
            Collection<String> instanceGroupNames = new HashSet<>();
            for (HostGroup hostGroup : hostGroups) {
                String instanceGroupName = hostGroup.getConstraint().getInstanceGroup().getGroupName();
                if (instanceGroupNames.contains(instanceGroupName)) {
                    throw new ClusterDefinitionValidationException(String.format(
                            "Instance group '%s' is assigned to more than one hostgroup.", instanceGroupName));
                }
                instanceGroupNames.add(instanceGroupName);
            }
            if (instanceGroups.size() < hostGroupsInRequest.size()) {
                throw new ClusterDefinitionValidationException("Each host group must have an instance group");
            }
        }
    }

    public void validateHostGroupScalingRequest(ClusterDefinition clusterDefinition, HostGroup hostGroup, Integer adjustment) {
        try {
            JsonNode hostGroupsNode = getHostGroupNode(clusterDefinition);
            Map<String, HostGroup> hostGroupMap = createHostGroupMap(Collections.singleton(hostGroup));
            for (JsonNode hostGroupNode : hostGroupsNode) {
                if (hostGroup.getName().equals(hostGroupNode.get("name").asText())) {
                    validateHostGroup(hostGroupNode, hostGroupMap, new HashMap<>(), adjustment);
                    break;
                }
            }
        } catch (IOException ignored) {
            throw new ClusterDefinitionValidationException(String.format("Blueprint [%s] can not be parsed from JSON.", clusterDefinition.getId()));
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
        Map<String, ClusterDefinitionServiceComponent> blueprintServiceComponentMap) {
        validateHostGroup(hostGroupNode, hostGroupMap, blueprintServiceComponentMap, 0);
    }

    private void validateHostGroup(JsonNode hostGroupNode, Map<String, HostGroup> hostGroupMap,
            Map<String, ClusterDefinitionServiceComponent> blueprintServiceComponentMap, int adjustment) {
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

    public HostGroup getHostGroup(Map<String, HostGroup> hostGroupMap, String hostGroupName) {
        return hostGroupMap.get(hostGroupName);
    }

    public JsonNode getComponentsNode(JsonNode hostGroupNode) {
        return hostGroupNode.get("components");
    }

    private void validateComponent(JsonNode componentNode, HostGroup hostGroup, Map<String, ClusterDefinitionServiceComponent> blueprintServiceComponentMap,
        int adjusment) {
        String componentName = componentNode.get("name").asText();
        StackServiceComponentDescriptor componentDescriptor = stackServiceComponentDescs.get(componentName);
        if (componentDescriptor != null) {
            validateComponentCardinality(componentDescriptor, hostGroup, adjusment);
            updateBlueprintServiceComponentMap(componentDescriptor, hostGroup, blueprintServiceComponentMap);
        }
    }

    private void validateComponentCardinality(StackServiceComponentDescriptor componentDescriptor, HostGroup hostGroup, int adjustment) {
        int nodeCount = hostGroup.getConstraint().getHostCount() + adjustment;
        int minCardinality = componentDescriptor.getMinCardinality();
        int maxCardinality = componentDescriptor.getMaxCardinality();
        if (componentDescriptor.isMaster() && !isNodeCountCorrect(nodeCount, minCardinality, maxCardinality)) {
            throw new ClusterDefinitionValidationException(String.format(
                    "The node count '%d' for hostgroup '%s' cannot be less than '%d' or more than '%d' because of '%s' component", nodeCount,
                    hostGroup.getName(), minCardinality, maxCardinality, componentDescriptor.getName()));
        }
    }

    private void validateBlueprintServiceComponents(Map<String, ClusterDefinitionServiceComponent> blueprintServiceComponentMap) {
        for (ClusterDefinitionServiceComponent clusterDefinitionServiceComponent : blueprintServiceComponentMap.values()) {
            String componentName = clusterDefinitionServiceComponent.getName();
            int nodeCount = clusterDefinitionServiceComponent.getNodeCount();
            StackServiceComponentDescriptor stackServiceComponentDescriptor = stackServiceComponentDescs.get(componentName);
            if (stackServiceComponentDescriptor != null) {
                int minCardinality = stackServiceComponentDescriptor.getMinCardinality();
                int maxCardinality = stackServiceComponentDescriptor.getMaxCardinality();
                if (!isNodeCountCorrect(nodeCount, minCardinality, maxCardinality) && !(nodeCount == 0 && !stackServiceComponentDescriptor.isMaster())) {
                    throw new ClusterDefinitionValidationException(
                            String.format("Incorrect number of '%s' components are in '%s' hostgroups: count: %d, min: %d max: %d",
                                    componentName, clusterDefinitionServiceComponent.getHostgroups().toString(), nodeCount, minCardinality, maxCardinality));
                }
            }
        }
    }

    private boolean isNodeCountCorrect(int nodeCount, int minCardinality, int maxCardinality) {
        return minCardinality <= nodeCount && nodeCount <= maxCardinality;
    }

    private void updateBlueprintServiceComponentMap(StackServiceComponentDescriptor componentDescriptor, HostGroup hostGroup,
            Map<String, ClusterDefinitionServiceComponent> blueprintServiceComponentMap) {
        String componentName = componentDescriptor.getName();
        ClusterDefinitionServiceComponent clusterDefinitionServiceComponent = blueprintServiceComponentMap.get(componentName);
        if (clusterDefinitionServiceComponent == null) {
            clusterDefinitionServiceComponent = new ClusterDefinitionServiceComponent(componentName, hostGroup.getName(),
                    hostGroup.getConstraint().getHostCount());
            blueprintServiceComponentMap.put(componentName, clusterDefinitionServiceComponent);
        } else {
            clusterDefinitionServiceComponent.update(hostGroup);
        }
    }
}
