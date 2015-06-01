package com.sequenceiq.cloudbreak.domain;

public enum CloudPlatform {
    AWS("ec2", true, 0, "xvd", 97),
    AZURE("azure", false, 6, "sd", 98),
    GCP("gcp", false, 30, "sd", 97),
    OPENSTACK("openstack", true, 0, "vd", 97);

    private final String initScriptPrefix;
    private final boolean withTemplate;
    private final Integer parallelNumber;
    private final String diskPrefix;
    private final Integer startLabel;

    private CloudPlatform(String initScriptPrefix, boolean withTemplate, Integer parallelNumber, String diskPrefix, Integer startLabel) {
        this.initScriptPrefix = initScriptPrefix;
        this.withTemplate = withTemplate;
        this.parallelNumber = parallelNumber;
        this.diskPrefix = diskPrefix;
        this.startLabel = startLabel;
    }

    public String getInitScriptPrefix() {
        return initScriptPrefix;
    }

    public String getDiskPrefix() {
        return diskPrefix;
    }

    public boolean isWithTemplate() {
        return withTemplate;
    }

    public Integer parallelNumber() {
        return parallelNumber;
    }

    public Integer startLabel() {
        return startLabel;
    }
}
