package com.sequenceiq.cloudbreak.common.metrics.type;

public enum MetricType implements Metric {
    VAULT_READ("vault.read"),
    VAULT_READ_FAILED("vault.read.failed"),
    VAULT_WRITE("vault.write"),
    VAULT_WRITE_FAILED("vault.write.failed"),
    VAULT_DELETE("vault.delete"),
    HEARTBEAT_UPDATE_SUCCESS("heartbeat.update.success"),
    HEARTBEAT_UPDATE_FAILED("heartbeat.update.failed"),
    REST_OPERATION("rest.operation"),
    REST_OPERATION_FAILED("rest.operation.failed"),
    DB_TRANSACTION_ID("db.transaction"),
    UMS_CALL_SUCCESS("ums.call.success"),
    UMS_CALL_FAILED("ums.call.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
