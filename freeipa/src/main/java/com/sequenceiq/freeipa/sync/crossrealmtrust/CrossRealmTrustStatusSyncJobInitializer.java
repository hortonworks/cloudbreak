package com.sequenceiq.freeipa.sync.crossrealmtrust;

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
public class CrossRealmTrustStatusSyncJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossRealmTrustStatusSyncJobInitializer.class);

    @Inject
    private CrossRealmTrustStatusSyncConfig config;

    @Inject
    private CrossRealmTrustStatusSyncJobService jobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        if (config.isEnabled()) {
            LOGGER.info("Scheduling CrossRealmTrustStatusSyncJobs");
            List<JobResource> jobResources = checkedMeasure(() ->
                    stackService.findAllWithCrossRealmTrustForAutoSync(), LOGGER, ":::CrossRealmTrustStatusSyncJob::: Stacks are fetched from db in {}ms");
            jobResources.forEach(jr -> jobService.schedule(new CrossRealmTrustStatusSyncJobAdapter(jr)));
            LOGGER.info("CrossRealmTrustStatusSyncJobInitializer is initiated with {} stacks on start", jobResources.size());
        } else {
            LOGGER.info("Skipping scheduling CrossRealmTrustStatusSyncJobs, as they are disabled.");
        }
    }
}
