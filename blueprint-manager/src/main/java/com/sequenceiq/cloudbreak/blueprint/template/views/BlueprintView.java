package com.sequenceiq.cloudbreak.blueprint.template.views;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public class BlueprintView {

    private String blueprintText;

    private String version;

    private String type;

    private Set<String> components;

    public BlueprintView(String blueprintText, String version, String type) {
        this.blueprintText = blueprintText;
        this.type = type;
        this.version = version;
        this.components = prepareComponents(blueprintText);
    }

    public BlueprintView(Cluster cluster, BlueprintStackInfo blueprintStackInfo) {
        this.blueprintText = cluster.getBlueprint().getBlueprintText();
        this.type = blueprintStackInfo.getType();
        this.version = blueprintStackInfo.getVersion();
        this.components = prepareComponents(cluster.getBlueprint().getBlueprintText());
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

    public Set<String> getComponents() {
        return components;
    }

    public void setComponents(Set<String> components) {
        this.components = components;
    }
}
