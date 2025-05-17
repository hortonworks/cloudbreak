package com.sequenceiq.cloudbreak.job.instancechecker.scheduler;

import static com.sequenceiq.cloudbreak.job.instancechecker.scheduler.InstanceCheckerSchedulerFactoryConfig.QUARTZ_INSTANCE_CHECKER_SCHEDULER;

import jakarta.inject.Inject;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

@Component("InstanceCheckerTransactionalScheduler")
public class InstanceCheckerTransactionalScheduler extends TransactionalScheduler {

    @Qualifier(QUARTZ_INSTANCE_CHECKER_SCHEDULER)
    @Inject
    private Scheduler scheduler;

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }
}
