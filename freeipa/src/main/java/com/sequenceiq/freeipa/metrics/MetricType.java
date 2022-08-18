package com.sequenceiq.freeipa.metrics;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum MetricType implements Metric {
    FREEIPA_CREATION_FINISHED("freeipa.creation.finished"),
    FREEIPA_CREATION_FAILED("freeipa.creation.failed"),
    AWS_VARIANT_MIGRATION_FAILED("aws.variant.migration.successful"),
    AWS_VARIANT_MIGRATION_SUCCESSFUL("aws.variant.migration.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
