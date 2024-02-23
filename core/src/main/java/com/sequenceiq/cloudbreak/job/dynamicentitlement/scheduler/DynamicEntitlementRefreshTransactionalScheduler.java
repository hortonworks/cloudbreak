package com.sequenceiq.cloudbreak.job.dynamicentitlement.scheduler;

import jakarta.inject.Inject;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

@Component("DynamicEntitlementRefreshTransactionalScheduler")
public class DynamicEntitlementRefreshTransactionalScheduler extends TransactionalScheduler {

    @Qualifier("dynamicEntitlementScheduler")
    @Inject
    private Scheduler scheduler;

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }
}
