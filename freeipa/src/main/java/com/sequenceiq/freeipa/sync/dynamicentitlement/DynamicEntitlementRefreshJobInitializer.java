package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class DynamicEntitlementRefreshJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshJobInitializer.class);

    @Inject
    private DynamicEntitlementRefreshConfig config;

    @Inject
    private DynamicEntitlementRefreshJobService jobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        if (config.isDynamicEntitlementEnabled()) {
            LOGGER.info("Scheduling dynamic entitlement setter jobs.");
            List<JobResource> jobResources = checkedMeasure(() ->
                    stackService.findAllForAutoSync(), LOGGER, ":::Auto sync::: Stacks are fetched from db in {}ms");
            for (JobResource jobResource : jobResources) {
                jobService.schedule(new DynamicEntitlementRefreshJobAdapter(jobResource));
            }
            LOGGER.info("DynamicEntitlementRefreshJobInitializer is inited with {} stacks on start", jobResources.size());
        } else {
            LOGGER.info("Skipping scheduling dynamic entitlement setter jobs, as they are disabled.");
        }

    }
}
