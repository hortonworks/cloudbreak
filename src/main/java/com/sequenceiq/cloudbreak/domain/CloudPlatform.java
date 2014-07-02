package com.sequenceiq.cloudbreak.domain;

public enum CloudPlatform {
    AWS("ec2"),
    AZURE("azure");

    private final String value;

    private CloudPlatform(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
