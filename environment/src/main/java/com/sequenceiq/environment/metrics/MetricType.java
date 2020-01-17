package com.sequenceiq.environment.metrics;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum MetricType implements Metric {
    ENV_CREATION_FINISHED("environment.creation.finished"),
    ENV_CREATION_FAILED("environment.creation.failed"),

    ENV_START_FINISHED("environment.start.finished"),
    ENV_START_FAILED("environment.start.failed"),

    ENV_STOP_FINISHED("environment.stop.finished"),
    ENV_STOP_FAILED("environment.stop.failed"),

    ENV_DELETION_FINISHED("environment.deletion.finished"),
    ENV_DELETION_FAILED("environment.deletion.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
