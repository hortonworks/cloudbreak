package com.sequenceiq.cloudbreak.job.existingstackfix;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ExistingStackFixerJobInitializer implements JobInitializer {

    @Inject
    private ExistingStackFixerConfig config;

    @Inject
    private ExistingStackFixerJobService jobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        if (!config.isExistingStackFixerEnabled()) {
            return;
        }
        stackService.getAllAlive().stream()
                .map(this::convertToStack)
                .filter(s -> !s.isStackInDeletionOrFailedPhase())
                .forEach(s -> jobService.schedule(new ExistingStackFixerJobAdapter(s)));
    }

    private Stack convertToStack(StackTtlView view) {
        Stack result = new Stack();
        result.setId(view.getId());
        result.setResourceCrn(view.getCrn());
        result.setStackStatus(view.getStatus());
        return result;
    }
}
