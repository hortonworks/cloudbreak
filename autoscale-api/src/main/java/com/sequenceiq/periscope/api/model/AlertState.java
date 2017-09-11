package com.sequenceiq.periscope.api.model;

public enum AlertState {
    OK("OK"),
    WARN("WARNING"),
    CRITICAL("CRITICAL");

    private final String value;

    AlertState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
