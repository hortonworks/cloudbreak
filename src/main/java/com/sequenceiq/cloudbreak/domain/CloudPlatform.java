package com.sequenceiq.cloudbreak.domain;

public enum CloudPlatform {
    AWS("ec2", true, 0),
    AZURE("azure", false, 6),
    GCC("gcc", false, 30),
    OPENSTACK("openstack", true, 0);

    private final String initScriptPrefix;
    private final boolean withTemplate;
    private final Integer parallelNumber;

    private CloudPlatform(String initScriptPrefix, boolean withTemplate, Integer parallelNumber) {
        this.initScriptPrefix = initScriptPrefix;
        this.withTemplate = withTemplate;
        this.parallelNumber = parallelNumber;
    }

    public String getInitScriptPrefix() {
        return initScriptPrefix;
    }

    public boolean isWithTemplate() {
        return withTemplate;
    }

    public Integer parallelNumber() {
        return parallelNumber;
    }
}
