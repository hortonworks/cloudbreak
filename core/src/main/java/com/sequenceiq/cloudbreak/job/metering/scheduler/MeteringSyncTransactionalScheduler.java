package com.sequenceiq.cloudbreak.job.metering.scheduler;

import static com.sequenceiq.cloudbreak.job.metering.scheduler.MeteringSyncSchedulerFactoryConfig.QUARTZ_METERING_SYNC_SCHEDULER;

import jakarta.inject.Inject;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

@Component("MeteringSyncTransactionalScheduler")
public class MeteringSyncTransactionalScheduler extends TransactionalScheduler {

    @Qualifier(QUARTZ_METERING_SYNC_SCHEDULER)
    @Inject
    private Scheduler scheduler;

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }
}
