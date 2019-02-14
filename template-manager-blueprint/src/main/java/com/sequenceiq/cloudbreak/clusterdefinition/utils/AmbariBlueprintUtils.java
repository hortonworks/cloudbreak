package com.sequenceiq.cloudbreak.clusterdefinition.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class AmbariBlueprintUtils {

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

    public int countHostTemplates(JsonNode root) {
        int hostTemplateCount = 0;
        Iterator<JsonNode> hostGroups = root.get("hostTemplates").elements();
        while (hostGroups.hasNext()) {
            hostGroups.next();
            hostTemplateCount++;
        }
        return hostTemplateCount;
    }

    public boolean isValidHostGroupName(String hostGroupName) {
        return StringUtils.isNotEmpty(hostGroupName) && validHostGroupNamePattern.matcher(hostGroupName).matches();
    }

    public boolean isAmbariBlueprint(String blueprint) {
        return isAmbariBlueprint(convertStringToJsonNode(blueprint));
    }

    public boolean isAmbariBlueprint(JsonNode blueprint) {
        return blueprint.path("cdhVersion").isMissingNode();
    }

    public String getBlueprintName(JsonNode root) {
        return root.get("Blueprints").get("blueprint_name").asText();
    }

    public String getCDHDisplayName(JsonNode root) {
        return root.get("displayName").asText();
    }

    public String getBlueprintStackVersion(JsonNode root) {
        return root.get("Blueprints").get("stack_version").asText();
    }

    public String getBlueprintStackName(JsonNode root) {
        return root.get("Blueprints").get("stack_name").asText();
    }

    public String getCDHStackVersion(JsonNode root) {
        return root.get("cdhVersion").asText();
    }

    public JsonNode convertStringToJsonNode(String json) {
        return jsonHelper.createJsonFromString(json);
    }

    public boolean isBlueprintNamePreConfigured(String blueprintStrings, String[] split) {
        return !blueprintStrings.isEmpty() && (split.length == 2 || split.length == 1) && !split[0].isEmpty();
    }

    public Map<String, Object> prepareTags(JsonNode tags) throws com.fasterxml.jackson.core.JsonProcessingException {
        Map<String, Object> map = new HashMap();
        if (tags != null) {
            map = JsonUtil.treeToValue(tags, Map.class);
        }
        return map;
    }

    public boolean isSharedServiceReadyBlueprint(ClusterDefinition clusterDefinition) {
        return clusterDefinition.getTags() != null && clusterDefinition.getTags().getMap().containsKey("shared_services_ready");
    }
}
