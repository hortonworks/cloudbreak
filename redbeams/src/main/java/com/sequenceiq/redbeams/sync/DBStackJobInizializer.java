package com.sequenceiq.redbeams.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class DBStackJobInizializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackJobInizializer.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackJobService dbStackJobService;

    @Override
    public void initJobs() {
        Set<DBStack> dbStacks = checkedMeasure(() -> dbStackService.findAllForAutoSync(), LOGGER, ":::Auto sync::: db stacks are fetched from db in {}ms");
        for (DBStack dbStack : dbStacks) {
            dbStackJobService.schedule(dbStack);
        }
        LOGGER.info("Auto syncer is inited with {} db stacks on start", dbStacks.size());
    }
}
