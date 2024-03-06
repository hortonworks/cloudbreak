package com.sequenceiq.cloudbreak.job.archiver.stack;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class StackArchiverJobInitializer implements JobInitializer {

    @Inject
    private StackArchiverJobService environmentArchiverJobService;

    @Override
    public void initJobs() {
        environmentArchiverJobService.schedule();
    }
}