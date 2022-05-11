package com.sequenceiq.freeipa.sync;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;

@Component
public class FreeipaJobService {

    @Inject
    private AutoSyncConfig autoSyncConfig;

    @Inject
    private StatusCheckerJobService jobService;

    public void schedule(JobResource jobResource) {
        if (autoSyncConfig.isEnabled()) {
            jobService.schedule(new StackJobAdapter(jobResource));
        }
    }

    public void schedule(Long id) {
        if (autoSyncConfig.isEnabled()) {
            jobService.schedule(id, StackJobAdapter.class);
        }
    }

    public void unschedule(Stack stack) {
        jobService.unschedule(String.valueOf(stack.getId()));
    }
}
