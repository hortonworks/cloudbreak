package com.sequenceiq.cdp.databus.quartz;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class DatabusCredentialCleanupJobInitializer implements JobInitializer {

    private final DatabusCredentialCleanuplJobConfig databusCredentialCleanupJobConfig;

    private final DatabusCredentialCleanupJobService databusCredentialCleanupJobService;

    public DatabusCredentialCleanupJobInitializer(DatabusCredentialCleanuplJobConfig databusCredentialCleanupJobConfig,
            DatabusCredentialCleanupJobService databusCredentialCleanupJobService) {
        this.databusCredentialCleanupJobConfig = databusCredentialCleanupJobConfig;
        this.databusCredentialCleanupJobService = databusCredentialCleanupJobService;
    }

    @Override
    public void initJobs() {
        if (databusCredentialCleanupJobConfig.isEnabled()) {
            databusCredentialCleanupJobService.schedule();
        } else {
            databusCredentialCleanupJobService.unschedule();
        }
    }
}
