package com.sequenceiq.cloudbreak.quartz.configuration;

import javax.inject.Inject;

import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.quartz.metric.JobMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.SchedulerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.TriggerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.ResourceCheckerJobListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

@Configuration
public class SchedulerFactoryConfig {

    @Inject
    private StatusCheckerConfig properties;

    @Inject
    private JobMetricsListener jobMetricsListener;

    @Inject
    private TriggerMetricsListener triggerMetricsListener;

    @Inject
    private ResourceCheckerJobListener resourceCheckerJobListener;

    @Inject
    private SchedulerMetricsListener schedulerMetricsListener;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setAutoStartup(properties.isAutoSyncEnabled());
            bean.setGlobalJobListeners(resourceCheckerJobListener, jobMetricsListener);
            bean.setGlobalTriggerListeners(triggerMetricsListener);
            bean.setSchedulerListeners(schedulerMetricsListener);
        };
    }

}
