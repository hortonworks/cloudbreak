package com.sequenceiq.freeipa.sync.saltstatus;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class StackSaltStatusJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackSaltStatusJobInitializer.class);

    @Inject
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    @Inject
    private StackService stackService;

    @Inject
    private StackSaltStatusCheckerJobService stackSaltStatusCheckerJobService;

    @Override
    public void initJobs() {
        if (saltStatusCheckerConfig.isEnabled()) {
            List<JobResource> jobResources = checkedMeasure(() ->
                    stackService.findAllForAutoSync(), LOGGER, ":::Auto sync::: Stacks are fetched from db in {}ms");
            for (JobResource jobResource : jobResources) {
                stackSaltStatusCheckerJobService.schedule(new StackSaltStatusCheckerJobAdapter(jobResource));
            }
            LOGGER.info("StackSaltStatusJobInitializer is inited with {} stacks on start", jobResources.size());
        }
    }
}
