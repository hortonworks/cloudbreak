package com.sequenceiq.cloudbreak.quartz.metric;

import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.SCHEDULER;

import org.quartz.SchedulerException;
import org.quartz.listeners.SchedulerListenerSupport;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

public class SchedulerMetricsListener extends SchedulerListenerSupport {

    private MetricService metricService;

    private String schedulerName;

    public SchedulerMetricsListener(MetricService metricService, String schedulerName) {
        this.metricService = metricService;
        this.schedulerName = schedulerName;
    }

    @Override
    public void schedulerError(String msg, SchedulerException cause) {
        getLog().warn("Scheduler {} error occured: {}", schedulerName, msg, cause);
        metricService.incrementMetricCounter(QuartzMetricType.SCHEDULER_ERROR,
                SCHEDULER.name(), schedulerName);
    }

    @Override
    public void schedulingDataCleared() {
        getLog().debug("Scheduling data cleared: {}", schedulerName);
    }
}
