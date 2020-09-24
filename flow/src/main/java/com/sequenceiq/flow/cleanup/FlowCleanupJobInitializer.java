package com.sequenceiq.flow.cleanup;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class FlowCleanupJobInitializer implements JobInitializer {

    @Inject
    private FlowCleanupJobService flowCleanupJobService;

    @Override
    public void initJobs() {
        flowCleanupJobService.schedule();
    }
}
