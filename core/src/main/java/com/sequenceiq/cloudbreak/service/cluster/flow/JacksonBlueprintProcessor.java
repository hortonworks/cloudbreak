package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.service.cluster.FileSystemConfigException;

@Component
public class JacksonBlueprintProcessor implements BlueprintProcessor {

    private static final String CONFIGURATIONS_NODE = "configurations";
    private static final String CORE_SITE_NODE = "core-site";
    private static final String DEFAULT_FS_KEY = "fs.defaultFs";

    @Override
    public String addConfigEntries(String originalBlueprint, List<BlueprintConfigurationEntry> configurationEntries) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectNode root = (ObjectNode) mapper.readTree(originalBlueprint);
            JsonNode configurationsNode = root.path(CONFIGURATIONS_NODE);
            if (configurationsNode.isMissingNode()) {
                configurationsNode = root.putArray(CONFIGURATIONS_NODE);
            }
            ArrayNode configurationsArrayNode = (ArrayNode) configurationsNode;
            for (BlueprintConfigurationEntry configurationEntry : configurationEntries) {
                JsonNode configFileNode = configurationsArrayNode.findPath(configurationEntry.getConfigFile());
                if (configFileNode.isMissingNode()) {
                    ObjectNode arrayElementNode = configurationsArrayNode.addObject();
                    configFileNode = arrayElementNode.putObject(configurationEntry.getConfigFile());

                }
                ((ObjectNode) configFileNode).put(configurationEntry.getKey(), configurationEntry.getValue());
            }
            return mapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new FileSystemConfigException("Failed to add filesystem config entries to original blueprint.", e);
        }
    }

    @Override
    public String addDefaultFs(String originalBlueprint, String defaultFs) {
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry(CORE_SITE_NODE, DEFAULT_FS_KEY, defaultFs));
        return addConfigEntries(originalBlueprint, configurationEntries);
    }
}
