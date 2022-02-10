package com.sequenceiq.cloudbreak.job;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;

@Component
public class StackJobInitializer extends AbstractStackJobInitializer {

    @Inject
    private StatusCheckerJobService jobService;

    @Override
    public void initJobs() {
        getAliveJobResources()
                .forEach(s -> jobService.schedule(new StackJobAdapter(s)));
    }
}
