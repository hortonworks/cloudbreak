package com.sequenceiq.cloudbreak.service.blueprint;

import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
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
}
