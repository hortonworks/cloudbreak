package com.sequenceiq.cloudbreak.quartz.metric;

import javax.inject.Inject;

import org.quartz.SchedulerException;
import org.quartz.listeners.SchedulerListenerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@Component
public class SchedulerMetricsListener extends SchedulerListenerSupport {

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Override
    public void schedulerError(String msg, SchedulerException cause) {
        getLog().warn("Scheduler error occured: {}", msg, cause);
        metricService.incrementMetricCounter(QuartzMetricType.SCHEDULER_ERROR);
    }

    @Override
    public void schedulingDataCleared() {
        getLog().debug("Scheduling data cleared!");
    }
}
