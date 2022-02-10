package com.sequenceiq.cloudbreak.job;

import java.util.stream.Stream;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractStackJobInitializer implements JobInitializer {

    @Inject
    private StackService stackService;

    protected Stream<Stack> getAliveStacksStream() {
        return stackService.getAllAlive().stream()
                .map(this::convertToStack);
    }

    protected Stream<Stack> getAliveAndNotDeleteInProgressStacksStream() {
        return getAliveStacksStream()
                .filter(s -> !s.isStackInDeletionOrFailedPhase());
    }

    private Stack convertToStack(StackTtlView view) {
        Stack result = new Stack();
        result.setId(view.getId());
        result.setResourceCrn(view.getCrn());
        result.setStackStatus(view.getStatus());
        return result;
    }
}
