package com.sequenceiq.freeipa.altus;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.cleanup.UMSCleanupConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;

@Component
public class FreeIpaUMSCleanupJobInitializer implements JobInitializer {

    @Inject
    private UMSCleanupConfig umsCleanupConfig;

    @Inject
    private FreeIpaUMSCleanupJobService freeIpaUmsCleanupJobService;

    @Override
    public void initJobs() {
        if (umsCleanupConfig.isEnabled()) {
            freeIpaUmsCleanupJobService.schedule();
        } else {
            freeIpaUmsCleanupJobService.unschedule();
        }
    }
}
