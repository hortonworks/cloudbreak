package com.sequenceiq.redbeams.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Set;

import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.statuschecker.model.JobInitializer;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

@Component
public class DBStackJobInizializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackJobInizializer.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackJobService dbStackJobService;

    @Override
    public void initJobs() {
        dbStackJobService.deleteAll();
        Set<DBStack> dbStacks = checkedMeasure(() -> dbStackService.findAllForAutoSync(), LOGGER, ":::Auto sync::: db stacks are fetched from db in {}ms");
        for (DBStack dbStack : dbStacks) {
            dbStackJobService.schedule(dbStack);
        }
        LOGGER.info("Auto syncer is inited with {} db stacks on start", dbStacks.size());
    }
}
