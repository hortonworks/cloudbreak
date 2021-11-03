package com.sequenceiq.cloudbreak.structuredevent.job;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;

@Component
public class StructuredSynchronizerJobInitializer extends AbstractStackJobInitializer {

    @Inject
    private StructuredSynchronizerJobService jobService;

    @Override
    public void initJobs() {
        getAliveStacksStream()
                .forEach(s -> jobService.scheduleWithDelay(new StructuredSynchronizerJobAdapter(s)));
    }
}