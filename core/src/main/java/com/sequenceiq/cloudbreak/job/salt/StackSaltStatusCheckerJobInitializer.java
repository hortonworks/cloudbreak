package com.sequenceiq.cloudbreak.job.salt;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.StaleAwareJobRescheduler;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;

@Component
public class StackSaltStatusCheckerJobInitializer extends AbstractStackJobInitializer implements StaleAwareJobRescheduler {

    @Inject
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    @Inject
    private StackSaltStatusCheckerJobService jobService;

    @Override
    public void initJobs() {
        if (saltStatusCheckerConfig.isEnabled()) {
            getAliveJobResources()
                    .forEach(s -> jobService.schedule(new StackSaltStatusCheckerJobAdapter(s)));
        }
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        jobService.schedule(id);
    }
}
