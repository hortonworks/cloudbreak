package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.io.IOException;
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
    private static final String HOST_GROUPS_NODE = "host_groups";

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
}
