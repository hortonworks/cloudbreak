package com.sequenceiq.cloudbreak.jvm;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum JvmMetricType implements Metric {

    JVM_NATIVE_MEMORY("jvm.native.memory");

    private final String metricName;

    JvmMetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
