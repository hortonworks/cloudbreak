package com.sequenceiq.periscope.monitor.evaluator;

public enum AlertState {
    OK("OK"),
    WARN("WARNING"),
    CRITICAL("CRITICAL");

    private String value;

    AlertState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
