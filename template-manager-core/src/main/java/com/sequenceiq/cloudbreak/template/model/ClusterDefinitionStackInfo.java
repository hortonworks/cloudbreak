package com.sequenceiq.cloudbreak.template.model;

public class ClusterDefinitionStackInfo {

    private String version;

    private String type;

    public ClusterDefinitionStackInfo(String version, String type) {
        this.version = version;
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }
}
