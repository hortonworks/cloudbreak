package com.sequenceiq.notification.scheduled.cleanup;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class NotificationCleanupJobInitializer implements JobInitializer {

    @Inject
    private NotificationCleanupJobService notificationCleanupJobService;

    @Override
    public void initJobs() {
        if (notificationCleanupJobService.enabled()) {
            notificationCleanupJobService.schedule();
        }
    }
}
