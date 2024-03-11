package com.sequenceiq.cloudbreak.quartz.metric;

import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.JOB_GROUP;
import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.SCHEDULER;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

@Component
public class JobCountMetricConfigurator {

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Inject
    private List<? extends JobSchedulerService> jobSchedulerServices;

    @PostConstruct
    public void init() {
        jobSchedulerServices.forEach(jobSchedulerService -> {
            TransactionalScheduler scheduler = jobSchedulerService.getScheduler();
            metricService.registerGaugeMetric(QuartzMetricType.JOB_COUNT, jobSchedulerService.getJobGroup(),
                    new GroupNameToJobCountFunction(scheduler), Map.of(JOB_GROUP.name(), jobSchedulerService.getJobGroup(),
                            SCHEDULER.name(), scheduler.getSchedulerName()));
        });
    }
}
