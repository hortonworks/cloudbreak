package com.sequenceiq.cloudbreak.job.metering;

import jakarta.inject.Inject;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.configuration.TransactionalScheduler;

@Component
public class MeteringSyncTransactionalScheduler extends TransactionalScheduler {

    @Qualifier("meteringSyncScheduler")
    @Inject
    private Scheduler scheduler;

    @Override
    protected Scheduler getScheduler() {
        return scheduler;
    }
}
