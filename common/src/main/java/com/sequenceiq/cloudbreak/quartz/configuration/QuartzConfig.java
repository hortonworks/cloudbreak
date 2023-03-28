package com.sequenceiq.cloudbreak.quartz.configuration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.quartz.SchedulerException;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:quartz.properties")
public class QuartzConfig {

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private JobAppVersionVerifier jobAppVersionVerifier;

    @PostConstruct
    public void configure() throws SchedulerException {
        scheduler.getListenerManager().addTriggerListener(jobAppVersionVerifier);
    }
}
