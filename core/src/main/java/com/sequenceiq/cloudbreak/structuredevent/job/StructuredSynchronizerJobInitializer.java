package com.sequenceiq.cloudbreak.structuredevent.job;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StructuredSynchronizerJobInitializer implements JobInitializer {

    @Inject
    private StructuredSynchronizerJobService jobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        stackService.getAllAlive().stream()
                .map(this::convertToStack)
                .forEach(s -> jobService.scheduleWithDelay(new StructuredSynchronizerJobAdapter(s)));
    }

    private Stack convertToStack(StackTtlView view) {
        Stack result = new Stack();
        result.setId(view.getId());
        result.setResourceCrn(view.getCrn());
        result.setStackStatus(view.getStatus());
        return result;
    }
}