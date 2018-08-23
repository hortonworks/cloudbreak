package com.sequenceiq.cloudbreak.blueprint.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintUtils {

    @Inject
    private JsonHelper jsonHelper;

    // regex for alphanumeric characters and underscores
    private final Pattern validHostGroupNamePattern = Pattern.compile("^\\w+$");

    public String readDefaultBlueprintFromFile(String[] split) throws IOException {
        return FileReaderUtils.readFileFromClasspath(String.format("defaults/blueprints/%s.bp", split.length == 2
                ? split[1].trim() : split[0].trim()));
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

    public boolean isValidHostGroupName(String hostGroupName) {
        return StringUtils.isNotEmpty(hostGroupName) && validHostGroupNamePattern.matcher(hostGroupName).matches();
    }

    public String getBlueprintName(JsonNode root) {
        return root.get("Blueprints").get("blueprint_name").asText();
    }

    public String getBlueprintStackVersion(JsonNode root) {
        return root.get("Blueprints").get("stack_version").asText();
    }

    public String getBlueprintStackName(JsonNode root) {
        return root.get("Blueprints").get("stack_name").asText();
    }

    public JsonNode convertStringToJsonNode(String json) {
        return jsonHelper.createJsonFromString(json);
    }

    public boolean isBlueprintNamePreConfigured(String blueprintStrings, String[] split) {
        return !blueprintStrings.isEmpty() && (split.length == 2 || split.length == 1) && !split[0].isEmpty();
    }

    public List<BlueprintParameter> prepareInputs(JsonNode inputs) throws com.fasterxml.jackson.core.JsonProcessingException {
        Set<BlueprintParameter> blueprintParameters = new HashSet<>();
        if (inputs != null && inputs.isArray()) {
            for (JsonNode objNode : inputs) {
                BlueprintParameter blueprintParameter = JsonUtil.treeToValue(objNode, BlueprintParameter.class);
                blueprintParameters.add(blueprintParameter);
            }
        }
        return new ArrayList<>(blueprintParameters);
    }

    public Map<String, Object> prepareTags(JsonNode tags) throws com.fasterxml.jackson.core.JsonProcessingException {
        Map<String, Object> map = new HashMap();
        if (tags != null) {
            map = JsonUtil.treeToValue(tags, Map.class);
        }
        return map;
    }

    public boolean isSharedServiceReqdyBlueprint(Blueprint blueprint) {
        return blueprint.getTags() != null && blueprint.getTags().getMap().containsKey("shared_services_ready");
    }
}
