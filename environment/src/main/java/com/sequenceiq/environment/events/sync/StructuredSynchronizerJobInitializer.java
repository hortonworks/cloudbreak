package com.sequenceiq.environment.events.sync;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class StructuredSynchronizerJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJobInitializer.class);

    @Inject
    private StructuredSynchronizerConfig structuredSynchronizerConfig;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private StructuredSynchronizerJobService jobService;

    @Override
    public void initJobs() {
        if (structuredSynchronizerConfig.isStructuredSyncEnabled()) {
            LOGGER.info("Starting structured synchronizer jobs");
            environmentService.findAllAliveForAutoSync(StructuredSynchronizerJob.unschedulableStates())
                    .forEach(env -> jobService.schedule(new StructuredSynchronizerJobAdapter(env), true));
        } else {
            LOGGER.info("Skipping scheduling structured synchronizer jobs, as they are disabled.");
        }
    }
}
