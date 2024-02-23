package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;

@Component
public class DynamicEntitlementRefreshJobInitializer extends AbstractStackJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshJobInitializer.class);

    @Inject
    private DynamicEntitlementRefreshConfig config;

    @Inject
    private DynamicEntitlementRefreshJobService jobService;

    @Override
    public void initJobs() {
        if (config.isDynamicEntitlementEnabled()) {
            LOGGER.info("Scheduling dynamic entitlement setter jobs.");
            getAliveJobResources()
                    .forEach(s -> jobService.schedule(new DynamicEntitlementRefreshJobAdapter(s)));
        } else {
            LOGGER.info("Skipping scheduling dynamic entitlement setter jobs, as they are disabled.");
        }

    }
}
