package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.statuschecker.model.JobInitializer;

@Component
public class StackJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackJobInitializer.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeipaJobService freeipaJobService;

    @Override
    public void initJobs() {
        freeipaJobService.deleteAll();
        List<Stack> stacks = checkedMeasure(() -> stackService.findAllForAutoSync(), LOGGER, ":::Auto sync::: stacks are fetched from db in {}ms");
        for (Stack stack : stacks) {
            freeipaJobService.schedule(stack);
        }
        LOGGER.info("Auto syncer is inited with {} stacks on start", stacks.size());
    }
}
