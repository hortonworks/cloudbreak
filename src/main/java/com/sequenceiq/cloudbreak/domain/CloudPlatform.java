package com.sequenceiq.cloudbreak.domain;

public enum CloudPlatform {
    AWS("ec2", true),
    AZURE("azure", false),
    GCC("gcc", false),
    OPENSTACK("openstack", true);

    private final String initScriptPrefix;
    private final boolean withTemplate;

    private CloudPlatform(String initScriptPrefix, boolean withTemplate) {
        this.initScriptPrefix = initScriptPrefix;
        this.withTemplate = withTemplate;
    }

    public String getInitScriptPrefix() {
        return initScriptPrefix;
    }

    public boolean isWithTemplate() {
        return withTemplate;
    }
}
