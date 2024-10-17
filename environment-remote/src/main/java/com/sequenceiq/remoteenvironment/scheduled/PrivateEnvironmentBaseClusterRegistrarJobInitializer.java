package com.sequenceiq.remoteenvironment.scheduled;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class PrivateEnvironmentBaseClusterRegistrarJobInitializer implements JobInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateEnvironmentBaseClusterRegistrarJobInitializer.class);

    @Inject
    private PrivateEnvironmentBaseClusterRegistrarJobService jobService;

    @Inject
    private PrivateEnvironmentBaseClusterRegistrarJobConfig jobConfig;

    @Override
    public void initJobs() {
        if (jobConfig.isEnabled()) {
            LOGGER.info("Initializing private environment base cluster registrar job.");
            jobService.schedule();
        } else {
            LOGGER.info("Private environment base cluster registrar job is disabled.");
        }
    }
}
