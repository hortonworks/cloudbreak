package com.sequenceiq.freeipa.sync;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.statuschecker.service.JobService;

@Component
public class FreeipaJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaJobService.class);

    @Inject
    private AutoSyncConfig autoSyncConfig;

    @Inject
    private JobService jobService;

    public void deleteAll() {
        jobService.deleteAll();
    }

    public void schedule(Stack stack) {
        if (autoSyncConfig.isEnabled()) {
            jobService.schedule(new StackJobAdapter(stack));
            LOGGER.info("{} is scheduled for auto sync", stack.getName());
        }
    }

    public void unschedule(Stack stack) {
        jobService.unschedule(new StackJobAdapter(stack).getLocalId());
        LOGGER.info("{} is unscheduled, it will not auto sync anymore", stack.getName());
    }
}
