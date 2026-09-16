package com.sequenceiq.maintenance.dispatcher.scheduled;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class MaintenanceWindowDispatcherJobInitializer implements JobInitializer {

    @Inject
    private MaintenanceWindowDispatcherJobService dispatcherJobService;

    @Override
    public void initJobs() {
        dispatcherJobService.schedule();
    }
}
