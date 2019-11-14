package com.sequenceiq.cloudbreak.telemetry.fluent;

public enum FluentClusterType {
    DATAHUB("datahub"), DATALAKE("datalake"), FREEIPA("freeipa");

    private String value;

    FluentClusterType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
