package com.sequenceiq.cloudbreak.job.dynamicentitlement.scheduler;

import static com.sequenceiq.cloudbreak.job.dynamicentitlement.scheduler.DynamicEntitlementRefreshSchedulerFactoryConfig.QUARTZ_DYNAMIC_ENTITLEMENT_REFRESH_SCHEDULER;

import jakarta.inject.Inject;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

@Component("DynamicEntitlementRefreshTransactionalScheduler")
public class DynamicEntitlementRefreshTransactionalScheduler extends TransactionalScheduler {

    @Qualifier(QUARTZ_DYNAMIC_ENTITLEMENT_REFRESH_SCHEDULER)
    @Inject
    private Scheduler scheduler;

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }
}
