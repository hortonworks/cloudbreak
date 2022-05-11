package com.sequenceiq.freeipa.nodestatus;

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
public class NodeStatusJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusJobInitializer.class);

    @Inject
    private NodeStatusJobConfig nodeStatusJobConfig;

    @Inject
    private NodeStatusJobService nodeStatusJobService;

    @Inject
    private StackService stackService;

    @Override
    public void initJobs() {
        if (nodeStatusJobConfig.isEnabled()) {
            List<JobResource> jobResources = checkedMeasure(() -> stackService.findAllForAutoSync(), LOGGER,
                    ":::Node status check::: Stacks are fetched from db in {}ms");
            for (JobResource jobResource : jobResources) {
                nodeStatusJobService.schedule(new NodeStatusJobAdapter(jobResource));
            }
            LOGGER.info("Node status checks are inited with {} stacks on start", jobResources.size());
        } else {
            LOGGER.info("Skipping scheduling node status checker jobs, as they are disabled");
        }
    }
}
