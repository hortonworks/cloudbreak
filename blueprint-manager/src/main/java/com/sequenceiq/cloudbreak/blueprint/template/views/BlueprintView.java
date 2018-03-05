package com.sequenceiq.cloudbreak.blueprint.template.views;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class BlueprintView {

    private String blueprintText;

    private Map<String, Object> blueprintInputs;

    private String version;

    private String type;

    public BlueprintView(String blueprintText, Map<String, Object> blueprintInputs, String version, String type) {
        this.blueprintText = blueprintText;
        this.blueprintInputs = blueprintInputs;
        this.type = type;
        this.version = version;
    }

    public BlueprintView(String blueprintText, String version, String type) {
        this.blueprintText = blueprintText;
        this.blueprintInputs = Maps.newHashMap();
        this.type = type;
        this.version = version;
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
    }

    public BlueprintView(String blueprintText, Json blueprintInputs, String version, String type) throws IOException {
        this.blueprintText = blueprintText;
        Map tmpblueprintInputs = blueprintInputs.get(Map.class);
        this.blueprintInputs = tmpblueprintInputs == null ? new HashMap<>() : tmpblueprintInputs;
        this.type = type;
        this.version = version;
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
}
