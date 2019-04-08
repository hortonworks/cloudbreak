package com.sequenceiq.cloudbreak.logger;

public enum LoggerContextKey {

    USER_ID("userId"),
    USER_NAME("userName"),
    TENANT("tenant"),
    WORKSPACE("workspace"),
    WORKSPACE_ID("workspaceId"),
    RESOURCE_TYPE("resourceType"),
    RESOURCE_ID("resourceId"),
    RESOURCE_NAME("resourceName"),
    FLOW_ID("flowId"),
    REQUEST_ID("requestId");

    private final String value;

    LoggerContextKey(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
