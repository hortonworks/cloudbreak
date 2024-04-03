package com.sequenceiq.remoteenvironment.scheduled;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class PrivateControlPlaneQueryJobInitializer implements JobInitializer {

    @Inject
    private PrivateControlPlaneQueryJobService privateControlPlaneQueryJobService;

    @Override
    public void initJobs() {
        privateControlPlaneQueryJobService.schedule();
    }
}
