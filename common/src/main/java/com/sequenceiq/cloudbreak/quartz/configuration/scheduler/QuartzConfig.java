package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.quartz.SchedulerException;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.sequenceiq.cloudbreak.quartz.configuration.JobAppVersionVerifier;

@Configuration
@PropertySource("classpath:quartz.properties")
public class QuartzConfig {

    @Inject
    private List<TransactionalScheduler> schedulers;

    @Inject
    private JobAppVersionVerifier jobAppVersionVerifier;

    @PostConstruct
    public void configure() throws SchedulerException {
        for (TransactionalScheduler scheduler : schedulers) {
            scheduler.getListenerManager().addTriggerListener(jobAppVersionVerifier);
        }
    }
}
