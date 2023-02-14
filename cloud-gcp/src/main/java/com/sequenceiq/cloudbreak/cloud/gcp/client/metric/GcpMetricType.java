package com.sequenceiq.cloudbreak.cloud.gcp.client.metric;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum GcpMetricType implements Metric {

    REQUEST_TIME("gcp.request.time");

    private final String metricName;

    GcpMetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
