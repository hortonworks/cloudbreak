package com.sequenceiq.node.health.client.model;

public enum CdpNodeStatusType {
    SYSTEM_METRICS("systemMetrics"), CM_METRICS_REPORT("cmMetricsReport");

    private final String value;

    CdpNodeStatusType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
