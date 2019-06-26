package com.sequenceiq.cloudbreak.logger;

public enum LoggerContextKey {
    WORKSPACE("workspace"),
    WORKSPACE_ID("workspaceId"),

    RESOURCE_ID("resourceId"),
    FLOW_ID("flowId"),
    REQUEST_ID("requestId"),
    ENVIRONMENT_CRN("environmentCrn"),
    RESOURCE_TYPE("resourceType"),
    RESOURCE_NAME("resourceName"),
    NAME("name"),
    RESOURCE_CRN("resourceCrn"),
    CRN("crn"),
    TENANT("tenant"),
    ACCOUNT_ID("accountId"),
    USER_CRN("userCrn");

    private final String value;

    LoggerContextKey(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
