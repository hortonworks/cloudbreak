package com.sequenceiq.cloudbreak.job;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.StaleAwareJobRescheduler;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;

@Component
public class StackJobInitializer extends AbstractStackJobInitializer implements StaleAwareJobRescheduler {

    @Inject
    private StatusCheckerJobService jobService;

    @Override
    public void initJobs() {
        getAliveJobResources()
                .forEach(s -> jobService.schedule(new StackJobAdapter(s)));
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        jobService.schedule(id, StackJobAdapter.class);
    }
}
