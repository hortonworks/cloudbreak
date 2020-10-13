package com.sequenceiq.cloudbreak.job.altus;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.cleanup.service.UMSCleanupJobService;

@Service
public class CloudbreakUMSCleanupJobService extends UMSCleanupJobService<CloudbreakUMSCleanupJob> {

    @Override
    public Class<CloudbreakUMSCleanupJob> getJobClass() {
        return CloudbreakUMSCleanupJob.class;
    }
}
