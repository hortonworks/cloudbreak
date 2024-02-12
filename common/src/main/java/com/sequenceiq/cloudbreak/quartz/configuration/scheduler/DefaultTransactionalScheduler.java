package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import jakarta.inject.Inject;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class DefaultTransactionalScheduler extends TransactionalScheduler {

    @Inject
    private Scheduler scheduler;

    public Scheduler getScheduler() {
        return scheduler;
    }
}
