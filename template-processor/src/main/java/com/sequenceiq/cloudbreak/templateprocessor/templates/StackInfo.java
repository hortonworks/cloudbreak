package com.sequenceiq.cloudbreak.templateprocessor.templates;

public class StackInfo {

    private String version;

    private String type;

    public StackInfo(String version, String type) {
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
