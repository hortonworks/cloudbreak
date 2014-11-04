package com.sequenceiq.cloudbreak.logger;

public enum LoggerResourceType {

    STACK_ID("stackId"),
    CLUSTER_ID("clusterId"),
    TEMPLATE_ID("templateId"),
    BLUEPRINT_ID("blueprintId"),
    CREDENTIAL_ID("credentialId"),
    CLOUDBREAK("cloudbreak");

    private final String value;

    LoggerResourceType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
