package com.sequenceiq.environment.environment.sync;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class EnvironmentJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentJobInitializer.class);

    private final EnvironmentJobService environmentJobService;

    private final EnvironmentService environmentService;

    public EnvironmentJobInitializer(EnvironmentJobService environmentJobService, EnvironmentService environmentService) {
        this.environmentJobService = environmentJobService;
        this.environmentService = environmentService;
    }

    @Override
    public void initJobs() {
        List<JobResource> jobResources = environmentService.findAllForAutoSync();
        for (JobResource jobResource : jobResources) {
            environmentJobService.schedule(jobResource);
        }
        LOGGER.info("Auto syncer is inited with {} environments on start", jobResources.size());
    }
}
