package com.sequenceiq.redbeams.sync.provider;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class RdsProviderSyncJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsProviderSyncJobInitializer.class);

    @Inject
    private RdsProviderSyncConfig config;

    @Inject
    private RdsProviderSyncJobService jobService;

    @Inject
    private DBStackService dbStackService;

    @Override
    public void initJobs() {
        if (config.isEnabled()) {
            Set<String> enabledProviders = config.getEnabledProviders();
            LOGGER.info("Scheduling RDS provider sync jobs on {}.", enabledProviders);
            Set<JobResource> jobResources = checkedMeasure(() -> dbStackService.findAllForAutoSync(), LOGGER,
                    ":::RDS provider sync::: db stacks are fetched from db in {}ms");

            long scheduledStacks = jobResources.stream()
                    .filter(jr -> enabledProviders.contains(jr.getProvider().orElse("")))
                    .peek(jr -> jobService.schedule(new RdsProviderSyncJobAdapter(jr)))
                    .count();

            LOGGER.info("RdsProviderSyncJobInitializer is initiated with {} db stacks on start", scheduledStacks);
        } else {
            LOGGER.info("Skipping scheduling RDS provider sync jobs, as they are disabled.");
        }
    }
}
