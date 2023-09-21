package com.sequenceiq.cloudbreak.job.metering;

import javax.inject.Inject;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.configuration.TransactionalScheduler;

@Component
public class MeteringTransactionalScheduler extends TransactionalScheduler {

    @Qualifier("meteringScheduler")
    @Inject
    private Scheduler scheduler;

    @Override
    protected Scheduler getScheduler() {
        return scheduler;
    }
}