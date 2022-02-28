package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class StackJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackJobInitializer.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeipaJobService freeipaJobService;

    @Override
    public void initJobs() {
        List<JobResource> jobResources = checkedMeasure(() -> stackService.findAllForAutoSync(), LOGGER, ":::Auto sync::: Stacks are fetched from db in {}ms");
        for (JobResource jobResource : jobResources) {
            freeipaJobService.schedule(jobResource);
        }
        LOGGER.info("Auto syncer is inited with {} stacks on start", jobResources.size());
    }
}
