package com.sequenceiq.cloudbreak.common.metrics.type;

public enum MetricType implements Metric {
    VAULT_READ("vault.read"),
    VAULT_WRITE("vault.write"),
    VAULT_DELETE("vault.delete"),
    HEARTBEAT_UPDATE_FAILED("heartbeat.update.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
