package com.sequenceiq.cloudbreak.quartz.metric;

import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.JOB_GROUP;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;

@Component
public class JobCountMetricConfigurator {

    @Inject
    private GroupNameToJobCountFunction groupNameToJobCountFunction;

    @Inject
    private List<MetricService> metricServices;

    @Inject
    private List<? extends JobSchedulerService> jobSchedulerServices;

    @PostConstruct
    public void init() {
        jobSchedulerServices.forEach(jobSchedulerService ->
                metricServices.forEach(metricService -> metricService.registerGaugeMetric(QuartzMetricType.JOB_COUNT, jobSchedulerService.getJobGroup(),
                        groupNameToJobCountFunction, Map.of(JOB_GROUP.name(), jobSchedulerService.getJobGroup()))));
}
}
