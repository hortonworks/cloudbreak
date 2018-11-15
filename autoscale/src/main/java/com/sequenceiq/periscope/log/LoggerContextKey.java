package com.sequenceiq.periscope.log;

public enum LoggerContextKey {

    RESOURCE_ID("resourceId"),
    CB_STACK_ID("cbStack");

    private final String value;

    LoggerContextKey(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
