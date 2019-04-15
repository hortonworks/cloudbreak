package com.sequenceiq.cloudbreak.service.metrics;

import com.sequenceiq.cloudbreak.common.type.metric.Metric;

public enum MetricType implements Metric {
    STACK_CREATION_SUCCESSFUL("stack.creation.successful"),
    STACK_CREATION_FAILED("stack.creation.failed"),
    STACK_UPSCALE_SUCCESSFUL("stack.upscale.successful"),
    STACK_UPSCALE_FAILED("stack.upscale.failed"),
    STACK_STOP_SUCCESSFUL("stack.stop.successful"),
    STACK_STOP_FAILED("stack.stop.failed"),
    STACK_START_SUCCESSFUL("stack.start.successful"),
    STACK_START_FAILED("stack.start.failed"),
    STACK_TERMINATION_SUCCESSFUL("stack.termination.successful"),
    STACK_TERMINATION_FAILED("stack.termination.failed"),

    CLUSTER_CREATION_SUCCESSFUL("cluster.creation.successful"),
    CLUSTER_CREATION_FAILED("cluster.creation.failed"),
    CLUSTER_UPSCALE_SUCCESSFUL("cluster.upscale.successful"),
    CLUSTER_UPSCALE_FAILED("cluster.upscale.failed"),
    CLUSTER_STOP_SUCCESSFUL("cluster.stop.successful"),
    CLUSTER_STOP_FAILED("cluster.stop.failed"),
    CLUSTER_START_SUCCESSFUL("cluster.start.successful"),
    CLUSTER_START_FAILED("cluster.start.failed"),

    STACK_PREPARATION("stack.preparation.duration"),

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
