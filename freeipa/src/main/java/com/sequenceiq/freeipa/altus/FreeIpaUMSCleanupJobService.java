package com.sequenceiq.freeipa.altus;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.cleanup.service.UMSCleanupJobService;

@Service
public class FreeIpaUMSCleanupJobService extends UMSCleanupJobService<FreeIpaUMSCleanupJob> {

    @Override
    public Class<FreeIpaUMSCleanupJob> getJobClass() {
        return FreeIpaUMSCleanupJob.class;
    }
}
