package com.sequenceiq.cloudbreak.job.diskusage;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.StaleAwareJobRescheduler;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class DiskUsageSyncJobInitializer extends AbstractStackJobInitializer implements StaleAwareJobRescheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskUsageSyncJobInitializer.class);

    @Inject
    private DiskUsageSyncConfig config;

    @Inject
    private DiskUsageSyncJobService jobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        if (config.isDiskUsageSyncEnabled()) {
            List<JobResource> aliveJobResources = getAliveJobResources().stream()
                    .toList();
            aliveJobResources
                    .forEach(s -> jobService.schedule(new DiskUsageSyncJobAdapter(s)));
            LOGGER.info("DiskUsageSyncJobInitializer is initiated with {} stacks on start", aliveJobResources.size());
        } else {
            LOGGER.info("Skipping scheduling disk usage sync jobs, as they are disabled.");
        }
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        JobResource jobResource = stackService.getJobResource(id);
        jobService.schedule(new DiskUsageSyncJobAdapter(jobResource));
    }
}