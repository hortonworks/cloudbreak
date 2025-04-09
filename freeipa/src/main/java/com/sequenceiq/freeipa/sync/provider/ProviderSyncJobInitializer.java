package com.sequenceiq.freeipa.sync.provider;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class ProviderSyncJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncJobInitializer.class);

    @Inject
    private ProviderSyncConfig config;

    @Inject
    private ProviderSyncJobService jobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        if (config.isProviderSyncEnabled()) {
            Set<String> enabledProviders = config.getEnabledProviders();
            LOGGER.info("Scheduling provider sync jobs on {}.", enabledProviders);
            List<JobResource> jobResources = checkedMeasure(() ->
                    stackService.findAllForAutoSync(), LOGGER, ":::Provider sync::: Stacks are fetched from db in {}ms");

            long scheduledStacks = jobResources.stream()
                    .filter(jr -> enabledProviders.contains(jr.getProvider().orElse("")))
                    .peek(jr -> jobService.schedule(new ProviderSyncJobAdapter(jr)))
                    .count();

            LOGGER.info("ProviderSyncJobInitializer is initiated with {} stacks on start", scheduledStacks);
        } else {
            LOGGER.info("Skipping scheduling provider sync jobs, as they are disabled.");
        }

    }
}