package com.sequenceiq.cloudbreak.domain;

public enum CloudPlatform {
    AWS("ec2"),
    AZURE("azure");

    private final String initScriptPrefix;

    private CloudPlatform(String initScriptPrefix) {
        this.initScriptPrefix = initScriptPrefix;
    }

    public String getInitScriptPrefix() {
        return initScriptPrefix;
    }
}
