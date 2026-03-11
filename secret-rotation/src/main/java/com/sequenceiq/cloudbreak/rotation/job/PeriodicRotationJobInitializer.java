package com.sequenceiq.cloudbreak.rotation.job;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties;
import com.sequenceiq.cloudbreak.rotation.service.periodic.PeriodicSecretRotationService;

@Component
@ConditionalOnBean(PeriodicSecretRotationService.class)
public class PeriodicRotationJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicRotationJobInitializer.class);

    @Inject
    private PeriodicRotationJobService scheduler;

    @Inject
    private PeriodicSecretRotationService rotationService;

    @Inject
    private PeriodicRotationProperties periodicRotationProperties;

    @Override
    public void initJobs() {
        if (!periodicRotationProperties.isEnabled()) {
            LOGGER.info("Skipping periodic secret rotation job initialization: disabled");
            return;
        }
        List<JobResource> jobResources = rotationService.listJobResources();
        for (JobResource jobResource : jobResources) {
            scheduler.schedule(new PeriodicRotationJobAdapter(jobResource));
        }
        LOGGER.info("Periodic secret rotation is initiated with {} resources on start", jobResources.size());
    }
}

