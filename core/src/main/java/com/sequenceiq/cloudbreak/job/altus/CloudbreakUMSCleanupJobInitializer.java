package com.sequenceiq.cloudbreak.job.altus;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.cleanup.UMSCleanupConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class CloudbreakUMSCleanupJobInitializer implements JobInitializer {

    @Inject
    private UMSCleanupConfig umsCleanupConfig;

    @Inject
    private CloudbreakUMSCleanupJobService cloudbreakUmsCleanupJobService;

    @Override
    public void initJobs() {
        if (umsCleanupConfig.isEnabled()) {
            cloudbreakUmsCleanupJobService.schedule();
        } else {
            cloudbreakUmsCleanupJobService.unschedule();
        }
    }
}
