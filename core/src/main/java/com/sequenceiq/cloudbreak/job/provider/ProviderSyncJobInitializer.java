package com.sequenceiq.cloudbreak.job.provider;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.StaleAwareJobRescheduler;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ProviderSyncJobInitializer extends AbstractStackJobInitializer implements StaleAwareJobRescheduler {

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
            List<JobResource> aliveJobResources = getAliveJobResources().stream()
                    .toList();
            aliveJobResources
                    .forEach(s -> jobService.schedule(new ProviderSyncJobAdapter(s)));
            LOGGER.info("ProviderSyncJobInitializer is initiated with {} stacks on start", aliveJobResources.size());
        } else {
            LOGGER.info("Skipping scheduling provider sync jobs, as they are disabled.");
        }
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        JobResource jobResource = stackService.getJobResource(id);
        jobService.schedule(new ProviderSyncJobAdapter(jobResource));
    }
}