package com.sequenceiq.cloudbreak.job.nodestatus;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class NodeStatusCheckerJobInitializer implements JobInitializer {

    @Inject
    private StackService stackService;

    @Inject
    private NodeStatusCheckerJobService nodeStatusCheckerJobService;

    @Override
    public void initJobs() {
        stackService.getAllAlive().stream()
                .map(this::convertToStack)
                .filter(s -> !s.isStackInDeletionOrFailedPhase())
                .forEach(s -> nodeStatusCheckerJobService.schedule(new NodeStatusJobAdapter(s)));
    }

    private Stack convertToStack(StackTtlView view) {
        Stack result = new Stack();
        result.setId(view.getId());
        result.setResourceCrn(view.getCrn());
        result.setStackStatus(view.getStatus());
        return result;
    }
}
