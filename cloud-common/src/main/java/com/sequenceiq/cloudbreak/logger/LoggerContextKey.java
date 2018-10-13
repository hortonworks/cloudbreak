package com.sequenceiq.cloudbreak.logger;

public enum LoggerContextKey {

    USER("user"),
    TENANT("tenant"),
    WORKSPACE("workspace"),
    RESOURCE_TYPE("resourceType"),
    RESOURCE_ID("resourceId"),
    RESOURCE_NAME("resourceName"),
    FLOW_ID("flowId"),
    TRACKING_ID("trackingId");

    private final String value;

    LoggerContextKey(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
