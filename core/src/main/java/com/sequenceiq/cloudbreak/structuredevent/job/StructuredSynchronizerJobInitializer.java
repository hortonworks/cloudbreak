package com.sequenceiq.cloudbreak.structuredevent.job;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;

@Component
public class StructuredSynchronizerJobInitializer extends AbstractStackJobInitializer {

    @Inject
    private StructuredSynchronizerJobService jobService;

    @Override
    public void initJobs() {
        getJobResourcesNotIn(Set.of(Status.DELETE_COMPLETED))
                .forEach(s -> jobService.scheduleWithDelay(new StructuredSynchronizerJobAdapter(s)));
    }
}