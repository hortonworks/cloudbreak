package com.sequenceiq.cloudbreak.job.cm;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.StaleAwareJobRescheduler;

@Component
public class ClouderaManagerSyncJobInitializer extends AbstractStackJobInitializer implements StaleAwareJobRescheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSyncJobInitializer.class);

    @Inject
    private ClouderaManagerSyncConfig config;

    @Inject
    private ClouderaManagerSyncJobService jobService;

    @Override
    public void initJobs() {
        if (config.isClouderaManagerSyncEnabled()) {
            List<JobResource> aliveJobResources = getAliveJobResources().stream()
                    .toList();
            aliveJobResources
                    .forEach(s -> jobService.schedule(new ClouderaManagerSyncJobAdapter(s)));
            LOGGER.info("ClouderaManagerSyncJobInitializer is initiated with {} stacks on start", aliveJobResources.size());
        } else {
            LOGGER.info("Skipping scheduling cm sync jobs, as they are disabled.");
        }
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        jobService.schedule(id);
    }
}