package com.sequenceiq.cloudbreak.telemetry.monitoring;

public enum MonitoringClusterType {

    CLOUDERA_MANAGER("cloudera_manager"), FREEIPA("freeipa");

    private String value;

    MonitoringClusterType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
