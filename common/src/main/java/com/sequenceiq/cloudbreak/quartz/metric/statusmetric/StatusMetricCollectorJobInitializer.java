package com.sequenceiq.cloudbreak.quartz.metric.statusmetric;

import javax.inject.Inject;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@ConditionalOnBean(StatusMetricCollector.class)
@Component
public class StatusMetricCollectorJobInitializer implements JobInitializer {

    @Inject
    private StatusMetricCollectorConfiguration statusMetricCollectorConfiguration;

    @Inject
    private StatusMetricCollectorJobService statusMetricCollectorJobService;

    @Override
    public void initJobs() {
        if (statusMetricCollectorConfiguration.isEnabled()) {
            statusMetricCollectorJobService.schedule();
        } else {
            statusMetricCollectorJobService.unschedule();
        }

    }
}