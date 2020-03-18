package com.sequenceiq.environment.environment.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.statuschecker.service.JobService;

@Component
public class EnvironmentJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentJobService.class);

    private final AutoSyncConfig autoSyncConfig;

    private final JobService jobService;

    public EnvironmentJobService(AutoSyncConfig autoSyncConfig, JobService jobService) {
        this.autoSyncConfig = autoSyncConfig;
        this.jobService = jobService;
    }

    public void deleteAll() {
        jobService.deleteAll();
    }

    public void schedule(Environment environment) {
        if (autoSyncConfig.isEnabled()) {
            jobService.schedule(new EnvironmentJobAdapter(environment));
            LOGGER.info("{} is scheduled for auto sync", environment.getName());
        }
    }

    public void unschedule(Long envId) {
        jobService.unschedule(envId.toString());
    }

    public void unschedule(Environment environment) {
        unschedule(environment.getId());
        LOGGER.info("{} is unscheduled, it will not auto sync anymore", environment.getName());
    }
}
