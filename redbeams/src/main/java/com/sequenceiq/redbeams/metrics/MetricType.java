package com.sequenceiq.redbeams.metrics;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum MetricType implements Metric {
    DB_PROVISION_FINISHED("db.provision.finished"),
    DB_PROVISION_FAILED("db.provision.failed"),
    DB_TERMINATION_FINISHED("db.termination.finished"),
    DB_TERMINATION_FAILED("db.termination.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
