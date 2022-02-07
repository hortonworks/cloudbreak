package com.sequenceiq.environment.environment.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;

@Component
public class EnvironmentJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentJobService.class);

    private final AutoSyncConfig autoSyncConfig;

    private final StatusCheckerJobService jobService;

    private final ApplicationContext applicationContext;

    public EnvironmentJobService(AutoSyncConfig autoSyncConfig, StatusCheckerJobService jobService, ApplicationContext applicationContext) {
        this.autoSyncConfig = autoSyncConfig;
        this.jobService = jobService;
        this.applicationContext = applicationContext;
    }

    public void schedule(JobResource jobResource) {
        if (autoSyncConfig.isEnabled()) {
            jobService.schedule(new EnvironmentJobAdapter(jobResource));
        }
    }

    public void schedule(Long id) {
        if (autoSyncConfig.isEnabled()) {
            EnvironmentJobAdapter resourceAdapter = new EnvironmentJobAdapter(id, applicationContext);
            jobService.schedule(resourceAdapter);
        }
    }

    public void unschedule(Long envId) {
        jobService.unschedule(envId.toString());
    }
}
