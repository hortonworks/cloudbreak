package com.sequenceiq.cloudbreak.job.metering.scheduler;

import static com.sequenceiq.cloudbreak.job.metering.scheduler.MeteringSchedulerFactoryConfig.QUARTZ_METERING_SCHEDULER;

import jakarta.inject.Inject;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

@Component("MeteringTransactionalScheduler")
public class MeteringTransactionalScheduler extends TransactionalScheduler {

    @Qualifier(QUARTZ_METERING_SCHEDULER)
    @Inject
    private Scheduler scheduler;

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }
}
