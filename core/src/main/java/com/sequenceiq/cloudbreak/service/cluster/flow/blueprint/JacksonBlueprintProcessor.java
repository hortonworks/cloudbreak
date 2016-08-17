package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class JacksonBlueprintProcessor implements BlueprintProcessor {

    private static final String CONFIGURATIONS_NODE = "configurations";
    private static final String SETTINGS_NODE = "settings";
    private static final String HOST_GROUPS_NODE = "host_groups";
    private static final String BLUEPRINTS = "Blueprints";
    private static final String STACK_VERSION = "stack_version";

    @Override
    public String addConfigEntries(String originalBlueprint, List<BlueprintConfigurationEntry> configurationEntries, boolean override) {
        try {
            ObjectNode root = (ObjectNode) JsonUtil.readTree(originalBlueprint);
            JsonNode configurationsNode = root.path(CONFIGURATIONS_NODE);
            if (configurationsNode.isMissingNode()) {
                configurationsNode = root.putArray(CONFIGURATIONS_NODE);
            }
            ArrayNode configurationsArrayNode = (ArrayNode) configurationsNode;
            for (BlueprintConfigurationEntry configurationEntry : configurationEntries) {
                JsonNode configFileNode = configurationsArrayNode.findPath(configurationEntry.getConfigFile());
                if (override || configFileNode.path("properties").findPath(configurationEntry.getKey()).isMissingNode()) {
                    if (configFileNode.isMissingNode()) {
                        ObjectNode arrayElementNode = configurationsArrayNode.addObject();
                        configFileNode = arrayElementNode.putObject(configurationEntry.getConfigFile());
                    }
                    JsonNode propertiesNode = configFileNode.path("properties");
                    if (!propertiesNode.isMissingNode()) {
                        ((ObjectNode) propertiesNode).put(configurationEntry.getKey(), configurationEntry.getValue());
                    } else {
                        ((ObjectNode) configFileNode).put(configurationEntry.getKey(), configurationEntry.getValue());
                    }
                }
            }
            return JsonUtil.writeValueAsString(root);
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to add config entries to original blueprint.", e);
        }
    }

    @Override
    public String addSettingsEntries(String originalBlueprint, List<BlueprintConfigurationEntry> configurationEntries, boolean override) {
        try {
            ObjectNode root = (ObjectNode) JsonUtil.readTree(originalBlueprint);
            JsonNode configurationsNode = root.path(SETTINGS_NODE);
            if (configurationsNode.isMissingNode()) {
                configurationsNode = root.putArray(SETTINGS_NODE);
            }
            ArrayNode configurationsArrayNode = (ArrayNode) configurationsNode;
            for (BlueprintConfigurationEntry configurationEntry : configurationEntries) {
                JsonNode configFileNode = configurationsArrayNode.findPath(configurationEntry.getConfigFile());
                if (override || configFileNode.findPath(configurationEntry.getKey()).isMissingNode()) {
                    if (configFileNode.isMissingNode()) {
                        ObjectNode arrayElementNode = configurationsArrayNode.addObject();
                        configFileNode = arrayElementNode.putObject(configurationEntry.getConfigFile());
                    }
                    ((ObjectNode) configFileNode).put(configurationEntry.getKey(), configurationEntry.getValue());
                }
            }
            return JsonUtil.writeValueAsString(root);
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to add config entries to original blueprint.", e);
        }
    }

    @Override
    public Set<String> getComponentsInHostGroup(String blueprintText, String hostGroup) {
        try {
            Set<String> services = new HashSet<>();
            ObjectNode root = (ObjectNode) JsonUtil.readTree(blueprintText);
            ArrayNode hostGroupsNode = (ArrayNode) root.path(HOST_GROUPS_NODE);
            Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
            while (hostGroups.hasNext()) {
                JsonNode hostGroupNode = hostGroups.next();
                if (hostGroup.equals(hostGroupNode.path("name").textValue())) {
                    Iterator<JsonNode> components = hostGroupNode.path("components").elements();
                    while (components.hasNext()) {
                        services.add(components.next().path("name").textValue());
                    }
                    break;
                }
            }
            return services;
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to get components for hostgroup '" + hostGroup + "' from blueprint.", e);
        }
    }

    @Override
    public boolean componentExistsInBlueprint(String component, String blueprintText) {
        boolean componentExists = false;
        try {
            ObjectNode root = (ObjectNode) JsonUtil.readTree(blueprintText);
            ArrayNode hostGroupsNode = (ArrayNode) root.path(HOST_GROUPS_NODE);
            Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
            while (hostGroups.hasNext() && !componentExists) {
                JsonNode hostGroupNode = hostGroups.next();
                componentExists = componentExistsInHostgroup(component, hostGroupNode);
            }
            return componentExists;
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to check that component('" + component + "') exists in the blueprint.", e);
        }
    }

    @Override
    public String removeComponentFromBlueprint(String component, String blueprintText) {
        try {
            ObjectNode root = (ObjectNode) JsonUtil.readTree(blueprintText);
            ArrayNode hostGroupsNode = (ArrayNode) root.path(HOST_GROUPS_NODE);
            Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
            while (hostGroups.hasNext()) {
                JsonNode hostGroupNode = hostGroups.next();
                Iterator<JsonNode> components = hostGroupNode.path("components").elements();
                while (components.hasNext()) {
                    if (component.equals(components.next().path("name").textValue())) {
                        components.remove();
                    }
                }
            }
            return JsonUtil.writeValueAsString(root);
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to remove component('" + component + "') from the blueprint.", e);
        }
    }

    @Override
    public String modifyHdpVersion(String blueprintText, String hdpVersion) {
        try {
            ObjectNode root = (ObjectNode) JsonUtil.readTree(blueprintText);
            ObjectNode blueprintsNode = (ObjectNode) root.path(BLUEPRINTS);
            blueprintsNode.remove(STACK_VERSION);
            String[] split = hdpVersion.split("\\.");
            blueprintsNode.put(STACK_VERSION, split[0] + "." + split[1]);
            return JsonUtil.writeValueAsString(root);
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to modify hdp version.", e);
        }
    }

    @Override
    public String addComponentToHostgroups(String component, Collection<String> hostGroupNames, String blueprintText) {
        try {
            ObjectNode root = (ObjectNode) JsonUtil.readTree(blueprintText);
            ArrayNode hostGroupsNode = (ArrayNode) root.path(HOST_GROUPS_NODE);
            Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
            while (hostGroups.hasNext()) {
                JsonNode hostGroupNode = hostGroups.next();
                String hostGroupName = hostGroupNode.path("name").textValue();
                if (hostGroupNames.contains(hostGroupName) && !componentExistsInHostgroup(component, hostGroupNode)) {
                    ArrayNode components = (ArrayNode) hostGroupNode.path("components");
                    components.addPOJO(new ComponentElement(component));
                }
            }
            return JsonUtil.writeValueAsString(root);
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to remove component('" + component + "') from the blueprint.", e);
        }
    }

    private boolean componentExistsInHostgroup(String component, JsonNode hostGroupNode) {
        boolean componentExists = false;
        Iterator<JsonNode> components = hostGroupNode.path("components").elements();
        while (components.hasNext()) {
            if (component.equals(components.next().path("name").textValue())) {
                componentExists = true;
                break;
            }
        }
        return componentExists;
    }

    private class ComponentElement {
        private String name;

        private ComponentElement(String component) {
            name = component;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
