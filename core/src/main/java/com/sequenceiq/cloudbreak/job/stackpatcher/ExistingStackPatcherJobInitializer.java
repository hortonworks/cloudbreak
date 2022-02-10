package com.sequenceiq.cloudbreak.job.stackpatcher;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;

@Component
public class ExistingStackPatcherJobInitializer extends AbstractStackJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJobInitializer.class);

    @Inject
    private ExistingStackPatcherConfig config;

    @Inject
    private ExistingStackPatcherJobService jobService;

    @Override
    public void initJobs() {
        if (config.isExistingStackPatcherEnabled()) {
            LOGGER.info("Scheduling stack patcher jobs");
            getAliveAndNotDeleteInProgressStacksStream()
                    .forEach(s -> jobService.schedule(new ExistingStackPatcherJobAdapter(s)));
        } else {
            LOGGER.info("Skipping scheduling stack patcher jobs, as they are disabled");
        }
    }
}
