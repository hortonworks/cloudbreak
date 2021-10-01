package com.sequenceiq.cloudbreak.job.raz;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.raz.service.RazSyncerJobService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class CloudbreakRazSyncerJobInitializer implements JobInitializer {

    @Inject
    private StackService stackService;

    @Inject
    private RazSyncerJobService jobService;

    @Override
    public void initJobs() {
        stackService.getAllAlive().stream()
                .map(this::convertToStack)
                .filter(s -> !s.isStackInDeletionOrFailedPhase())
                .forEach(s -> jobService.schedule(new CloudbreakRazSyncerJobAdapter(s)));
    }

    private Stack convertToStack(StackTtlView view) {
        Stack result = new Stack();
        result.setId(view.getId());
        result.setResourceCrn(view.getCrn());
        result.setStackStatus(view.getStatus());
        return result;
    }
}
