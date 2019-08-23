package com.sequenceiq.cloudbreak.logger;

public enum LoggerContextKey {
    WORKSPACE("workspace"),
    WORKSPACE_ID("workspaceId"),

    RESOURCE_ID("resourceId"),
    FLOW_ID("flowId"),
    REQUEST_ID("requestId"),
    ENVIRONMENT_CRN("environmentCrn"),
    ENV_CRN("envCrn"),
    RESOURCE_TYPE("resourceType"),
    RESOURCE_NAME("resourceName"),
    NAME("name"),
    CLUSTER_NAME("clusterName"),
    RESOURCE_CRN("resourceCrn"),
    CRN("crn"),
    TENANT("tenant"),
    ACCOUNT_ID("accountId"),
    USER_CRN("userCrn"),
    TRACE_ID("traceId"),
    SPAN_ID("spanId");

    private final String value;

    LoggerContextKey(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
