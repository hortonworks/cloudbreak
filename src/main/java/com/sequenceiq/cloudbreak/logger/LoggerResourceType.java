package com.sequenceiq.cloudbreak.logger;

public enum LoggerResourceType {

    STACK("stack"),
    CLUSTER("cluster"),
    TEMPLATE("template"),
    BLUEPRINT("blueprint"),
    CREDENTIAL("credential"),
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
