package com.sequenceiq.cloudbreak.quartz.metric;

import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.JOB_GROUP;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@Component
public class JobCountMetricConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCountMetricConfigurator.class);

    @Inject
    private GroupNameToJobCountFunction groupNameToJobCountFunction;

    @Inject
    private Scheduler scheduler;

    @Inject
    private MetricService metricService;

    @PostConstruct
    public void init() {
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                metricService.registerGaugeMetric(QuartzMetricType.JOB_COUNT, groupName, groupNameToJobCountFunction,
                        Map.of(JOB_GROUP.name(), groupName));
            }
        } catch (SchedulerException e) {
            LOGGER.error("Cannot create job count metrics", e);
        }
    }
}
