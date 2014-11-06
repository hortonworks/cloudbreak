package com.sequenceiq.cloudbreak.logger;

public enum LoggerContextKey {

    OWNER_ID("owner"),
    RESOURCE_TYPE("resourceType"),
    RESOURCE_ID("resourceId"),
    RESOURCE_NAME("resourceName");

    private final String value;

    LoggerContextKey(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
