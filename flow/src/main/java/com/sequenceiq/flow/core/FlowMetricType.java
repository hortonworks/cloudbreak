package com.sequenceiq.flow.core;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum FlowMetricType implements Metric {
    FLOW_STEP("flowstep"),
    ACTIVE_FLOWS("activeflow"),
    FLOW_STARTED("flow.started"),
    FLOW_FINISHED("flow.finished"),
    FLOW_FAILED("flow.failed");

    private final String metricName;

    FlowMetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
