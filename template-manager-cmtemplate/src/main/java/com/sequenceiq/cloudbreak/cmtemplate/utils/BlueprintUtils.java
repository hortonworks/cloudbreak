package com.sequenceiq.cloudbreak.cmtemplate.utils;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class BlueprintUtils {

    public static final String BLUEPRINTS_JSON_NODE_TEXT = "Blueprints";

    public static final String CDH_VERSION_JSON_NODE_TEXT = "cdhVersion";

    private static final String BLUEPRINT_NODE = "blueprint";

    private static final String SDX_ENTERPRISE_DATALAKE_TEXT = "Enterprise SDX";

    @Inject
    private JsonHelper jsonHelper;

    public String readDefaultBlueprintFromFile(String version, String[] split) throws IOException {
        return FileReaderUtils.readFileFromClasspath(String.format("defaults/blueprints/%s/%s.bp", version, split.length == 2
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

    public String getBlueprintVariant(String blueprint) {
        return ClusterApi.CLOUDERA_MANAGER;
    }

    public boolean isClouderaManagerClusterTemplate(String text) {
        return isClouderaManagerClusterTemplate(convertStringToJsonNode(text));
    }

    public boolean isClouderaManagerClusterTemplate(JsonNode blueprint) {
        return blueprint.path(CDH_VERSION_JSON_NODE_TEXT).isValueNode() && blueprint.path(BLUEPRINTS_JSON_NODE_TEXT).isMissingNode();
    }

    public boolean isAmbariBlueprint(JsonNode blueprint) {
        return blueprint.path(CDH_VERSION_JSON_NODE_TEXT).isMissingNode() && blueprint.path(BLUEPRINTS_JSON_NODE_TEXT).isContainerNode();
    }

    public boolean isBuiltinBlueprint(JsonNode root) {
        JsonNode blueprint = getBuiltinBlueprintContent(root);
        return blueprint != null
                && (isClouderaManagerClusterTemplate(blueprint) || isAmbariBlueprint(blueprint));
    }

    public JsonNode getBuiltinBlueprintContent(JsonNode root) {
        return root.get(BLUEPRINT_NODE);
    }

    public String getBlueprintName(JsonNode root) {
        return root.get(BLUEPRINTS_JSON_NODE_TEXT).get("blueprint_name").asText();
    }

    public String getCDHDisplayName(JsonNode root) {
        return root.get("displayName").asText();
    }

    public String getBlueprintStackVersion(JsonNode root) {
        if (root.get(BLUEPRINTS_JSON_NODE_TEXT) != null) {
            return root.get(BLUEPRINTS_JSON_NODE_TEXT).get("stack_version").asText();
        }
        return "";
    }

    public String getBlueprintStackName(JsonNode root) {
        if (root.get(BLUEPRINTS_JSON_NODE_TEXT) != null) {
            return root.get(BLUEPRINTS_JSON_NODE_TEXT).get("stack_name").asText();
        }
        return "";
    }

    public String getCDHStackVersion(JsonNode root) {
        return root.get(CDH_VERSION_JSON_NODE_TEXT).asText();
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

    public boolean isSharedServiceReadyBlueprint(Blueprint blueprint) {
        return blueprint.getTags() != null && blueprint.getTags().getMap().containsKey("shared_services_ready");
    }

    public BlueprintUpgradeOption getBlueprintUpgradeOptionForEnabled() {
        return BlueprintUpgradeOption.ENABLED;
    }

    public BlueprintUpgradeOption getBlueprintUpgradeOptionForEnabled(JsonNode blueprintJson) {
        return Optional.ofNullable(blueprintJson)
                .map(json -> json.get("blueprintUpgradeOption"))
                .map(JsonNode::asText)
                .map(BlueprintUpgradeOption::valueOf)
                .orElse(BlueprintUpgradeOption.ENABLED);
    }

    public BlueprintHybridOption getHybridOption(JsonNode blueprintJson) {
        return Optional.ofNullable(blueprintJson)
                .map(json -> json.get("hybridOption"))
                .map(JsonNode::asText)
                .map(BlueprintHybridOption::valueOf)
                .orElse(BlueprintHybridOption.NONE);
    }

    public boolean isEnterpriseDatalake(Stack stack) {
        return (stack.isDatalake() &&
                containsIgnoreCase(stack.getCluster().getBlueprint().getDescription(), SDX_ENTERPRISE_DATALAKE_TEXT));
    }

    public boolean isEnterpriseDatalake(TemplatePreparationObject templatePreparationObject) {
        return containsIgnoreCase(templatePreparationObject.getBlueprintView().getBlueprintText(), SDX_ENTERPRISE_DATALAKE_TEXT);
    }
}
