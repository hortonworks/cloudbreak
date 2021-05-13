package com.sequenceiq.cloudbreak.quartz.metric;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class JobCountMetricConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCountMetricConfigurator.class);

    @Inject
    private GroupNameToJobCountFunction groupNameToJobCountFunction;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private Scheduler scheduler;

    @PostConstruct
    public void init() {
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                Gauge.builder(QuartzMetricType.JOB_COUNT.getMetricName(), groupName, groupNameToJobCountFunction)
                        .tags("group", groupName)
                        .register(meterRegistry);
            }
        } catch (SchedulerException e) {
            LOGGER.error("Cannot create job count metrics", e);
        }
    }
}
