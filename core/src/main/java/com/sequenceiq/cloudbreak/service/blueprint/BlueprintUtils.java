package com.sequenceiq.cloudbreak.service.blueprint;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintUtils {

    @Inject
    private JsonHelper jsonHelper;

    public String readDefaultBlueprintFromFile(String[] split) throws IOException {
        return FileReaderUtils.readFileFromClasspath(String.format("defaults/blueprints/%s.bp", split.length == 2 ? split[1] : split[0]));
    }

    public int countHostGroups(JsonNode root) {
        int hostGroupCount = 0;
        Iterator<JsonNode> hostGroups = root.get("host_groups").elements();
        while (hostGroups.hasNext()) {
            hostGroups.next();
            hostGroupCount++;
        }
        return hostGroupCount;
    }

    public String getBlueprintName(JsonNode root) {
        return root.get("Blueprints").get("blueprint_name").asText();
    }

    public String getBlueprintHdpVersion(JsonNode root) {
        return root.get("Blueprints").get("stack_version").asText();
    }

    public String getBlueprintStackName(JsonNode root) {
        return root.get("Blueprints").get("stack_name").asText();
    }

    public JsonNode convertStringToJsonNode(String json) {
        return jsonHelper.createJsonFromString(json);
    }

    public Boolean containsComponent(Blueprint blueprint, String componentNm) throws IOException {
        JsonNode blueprintNode = JsonUtil.readTree(blueprint.getBlueprintText());
        JsonNode hostGroups = blueprintNode.path("host_groups");
        for (JsonNode hostGroup : hostGroups) {
            JsonNode components = hostGroup.path("components");
            for (JsonNode component : components) {
                String name = component.path("name").asText();
                if (name.equalsIgnoreCase(componentNm)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isBlueprintNamePreConfigured(String blueprintStrings, String[] split) {
        return !blueprintStrings.isEmpty() && (split.length == 2 || split.length == 1) && !split[0].isEmpty();
    }

    public List<BlueprintParameter> prepareInputs(JsonNode inputs) throws com.fasterxml.jackson.core.JsonProcessingException {
        Set<BlueprintParameter> blueprintParameters = new HashSet<>();
        if (inputs.isArray()) {
            for (final JsonNode objNode : inputs) {
                BlueprintParameter blueprintParameter = JsonUtil.treeToValue(objNode, BlueprintParameter.class);
                blueprintParameters.add(blueprintParameter);
            }
        }
        return blueprintParameters.stream().collect(Collectors.toList());
    }
}
