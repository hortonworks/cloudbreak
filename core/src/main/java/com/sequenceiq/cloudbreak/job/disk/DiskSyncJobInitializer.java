package com.sequenceiq.cloudbreak.job.disk;

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
public class DiskSyncJobInitializer extends AbstractStackJobInitializer implements StaleAwareJobRescheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSyncJobInitializer.class);

    @Inject
    private DiskSyncConfig config;

    @Inject
    private DiskSyncJobService jobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        if (config.isDiskSyncEnabled()) {
            Set<String> enabledProviders = config.getEnabledProviders();
            LOGGER.info("Scheduling disk sync jobs on {}.", enabledProviders);
            List<JobResource> aliveJobResources = getAliveJobResources().stream()
                    .toList();
            aliveJobResources
                    .forEach(s -> jobService.schedule(new DiskSyncJobAdapter(s)));
            LOGGER.info("DiskSyncJobInitializer is initiated with {} stacks on start", aliveJobResources.size());
        } else {
            LOGGER.info("Skipping scheduling provider sync jobs, as they are disabled.");
        }
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        JobResource jobResource = stackService.getJobResource(id);
        jobService.schedule(new DiskSyncJobAdapter(jobResource));
    }
}