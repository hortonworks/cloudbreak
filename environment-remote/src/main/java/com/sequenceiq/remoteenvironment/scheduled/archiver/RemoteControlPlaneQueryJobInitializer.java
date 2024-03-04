package com.sequenceiq.remoteenvironment.scheduled.archiver;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class RemoteControlPlaneQueryJobInitializer implements JobInitializer {

    @Inject
    private RemoteControlPlaneQueryJobService remoteControlPlaneQueryJobService;

    @Override
    public void initJobs() {
        remoteControlPlaneQueryJobService.schedule();
    }
}
