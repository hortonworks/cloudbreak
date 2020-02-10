package com.sequenceiq.cloudbreak.job;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.statuschecker.model.JobInitializer;
import com.sequenceiq.statuschecker.service.JobService;

@Component
public class StackJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackJobInitializer.class);

    @Inject
    private JobService jobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        jobService.deleteAll();
        stackService.getAllAlive().stream()
                .map(this::convertToStack)
                .filter(s -> !s.isStackInDeletionOrFailedPhase())
                .forEach(s -> jobService.schedule(new StackJobAdapter(s)));
    }

    private Stack convertToStack(StackTtlView view) {
        Stack result = new Stack();
        result.setId(view.getId());
        result.setResourceCrn(view.getCrn());
        result.setStackStatus(view.getStatus());
        return result;
    }
}
