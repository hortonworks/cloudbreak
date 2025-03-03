package com.sequenceiq.freeipa.events.sync;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class StructuredSynchronizerJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJobInitializer.class);

    @Inject
    private StructuredSynchronizerConfig structuredSynchronizerConfig;

    @Inject
    private StackService stackService;

    @Inject
    private StructuredSynchronizerJobService jobService;

    @Override
    public void initJobs() {
        if (structuredSynchronizerConfig.isStructuredSyncEnabled()) {
            LOGGER.info("Starting structured synchronizer jobs");
            stackService.findAllAliveForAutoSync(StructuredSynchronizerJob.unschedulableStates())
                    .forEach(stack -> jobService.schedule(new StructuredSynchronizerJobAdapter(stack), true));
        } else {
            LOGGER.info("Skipping scheduling structured synchronizer jobs, as they are disabled.");
        }
    }
}
