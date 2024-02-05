package com.sequenceiq.environment.environment.scheduled.archiver;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class EnvironmentArchiverJobInitializer implements JobInitializer {

    @Inject
    private EnvironmentArchiverJobService environmentArchiverJobService;

    @Override
    public void initJobs() {
        environmentArchiverJobService.schedule();
    }
}
