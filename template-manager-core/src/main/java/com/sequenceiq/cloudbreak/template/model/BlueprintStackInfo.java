package com.sequenceiq.cloudbreak.template.model;

public class BlueprintStackInfo {

    private final String version;

    private final String type;

    public BlueprintStackInfo(String version, String type) {
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
