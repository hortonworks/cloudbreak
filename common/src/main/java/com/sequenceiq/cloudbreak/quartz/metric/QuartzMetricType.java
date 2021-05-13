package com.sequenceiq.cloudbreak.quartz.metric;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum QuartzMetricType implements Metric {
    JOB_TRIGGERED("quartz.job.triggered"),
    JOB_FINISHED("quartz.job.finished"),
    JOB_FAILED("quartz.job.failed"),
    JOB_COUNT("quartz.job.count"),
    TRIGGER_FIRED("quartz.trigger.fired"),
    TRIGGER_MISFIRED("quartz.trigger.misfired"),
    TRIGGER_COMPLETED("quartz.trigger.completed"),
    TRIGGER_DELAYED("quartz.trigger.delayed");

    private final String metricName;

    QuartzMetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
