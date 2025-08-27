package com.sequenceiq.cloudbreak.job.status;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.statuscleanup.StackStatusCleanupJobService;

@Component
public class StackStatusCleanupJobInitializer implements JobInitializer {

    @Inject
    private StackStatusCleanupJobService stackStatusCleanUpJobService;

    @Override
    public void initJobs() {
        if (stackStatusCleanUpJobService.isCleanupEnabled()) {
            stackStatusCleanUpJobService.schedule();
        } else {
            stackStatusCleanUpJobService.unschedule();
        }
    }
}