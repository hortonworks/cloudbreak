package com.sequenceiq.cloudbreak.job;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        Iterable<Stack> clusters = stackService.getAllAliveWithInstanceGroups();
        jobService.deleteAll();
        for (Stack cluster : clusters) {
            if (!cluster.isStackInDeletionOrFailedPhase()) {
                jobService.schedule(new StackJobAdapter(cluster));
            }
        }
    }
}
