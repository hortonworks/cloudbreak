package com.sequenceiq.cloudbreak.blueprint.template.views;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class BlueprintView {

    private String blueprintText;

    private Map<String, Object> blueprintInputs;

    private String version;

    private String type;

    private Set<String> components;

    public BlueprintView(String blueprintText, Map<String, Object> blueprintInputs, String version, String type) {
        this.blueprintText = blueprintText;
        this.blueprintInputs = blueprintInputs;
        this.type = type;
        this.version = version;
        this.components = prepareComponents(blueprintText);
    }

    public BlueprintView(String blueprintText, String version, String type) {
        this.blueprintText = blueprintText;
        this.blueprintInputs = Maps.newHashMap();
        this.type = type;
        this.version = version;
        this.components = prepareComponents(blueprintText);
    }

    public BlueprintView(Cluster cluster, BlueprintStackInfo blueprintStackInfo) throws IOException {
        this.blueprintText = cluster.getBlueprint().getBlueprintText();
        Map tmpblueprintInputs = cluster.getBlueprintInputs().get(Map.class);
        if (tmpblueprintInputs == null) {
            this.blueprintInputs = new HashMap<>();
        } else {
            this.blueprintInputs = tmpblueprintInputs;
        }
        this.type = blueprintStackInfo.getType();
        this.version = blueprintStackInfo.getVersion();
        this.components = prepareComponents(cluster.getBlueprint().getBlueprintText());
    }

    public BlueprintView(String blueprintText, Json blueprintInputs, String version, String type) throws IOException {
        this.blueprintText = blueprintText;
        prepareComponents(blueprintText);
        Map tmpblueprintInputs = blueprintInputs.get(Map.class);
        this.blueprintInputs = tmpblueprintInputs == null ? new HashMap<>() : tmpblueprintInputs;
        this.type = type;
        this.version = version;
        this.components = prepareComponents(blueprintText);
    }

    private Set<String> prepareComponents(String blueprintText) {
        Set<String> result = new HashSet<>();
        try {
            BlueprintTextProcessor blueprintTextProcessor = new BlueprintTextProcessor(blueprintText);
            Map<String, Set<String>> componentsByHostGroup = blueprintTextProcessor.getComponentsByHostGroup();
            componentsByHostGroup.values().forEach(result::addAll);
        } catch (BlueprintProcessingException exception) {
            result = new HashSet<>();
        }
        return result;
    }

    public String getVersion() {
        return version;
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = blueprintText;
    }

    public void setBlueprintInputs(Map<String, Object> blueprintInputs) {
        this.blueprintInputs = blueprintInputs;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public boolean isHdf() {
        return "HDF".equals(type.toUpperCase());
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public Map<String, Object> getBlueprintInputs() {
        return blueprintInputs;
    }

    public Set<String> getComponents() {
        return components;
    }

    public void setComponents(Set<String> components) {
        this.components = components;
    }
}
