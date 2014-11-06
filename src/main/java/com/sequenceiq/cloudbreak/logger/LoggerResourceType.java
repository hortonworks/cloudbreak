package com.sequenceiq.cloudbreak.logger;

public enum LoggerResourceType {

    STACK("STACK"),
    CLUSTER("CLUSTER"),
    TEMPLATE("TEMPLATE"),
    BLUEPRINT("BLUEPRINT"),
    CREDENTIAL("CREDENTIAL"),
    EVENT("EVENT"),
    EVENT_DATA("EVENT_DATA"),
    USAGE("USAGE"),
    CLOUDBREAK_APPLICATION("CLOUDBREAK_APPLICATION");

    private final String value;

    LoggerResourceType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
