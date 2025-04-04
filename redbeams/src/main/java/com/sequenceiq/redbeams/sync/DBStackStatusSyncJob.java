package com.sequenceiq.redbeams.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@DisallowConcurrentExecution
public class DBStackStatusSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackStatusSyncJob.class);

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusSyncService dbStackStatusSyncService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        Long dbStackId = Long.valueOf(getLocalId());
        return Optional.ofNullable(dbStackService.getById(dbStackId));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        Long dbStackId = Long.valueOf(getLocalId());
        DBStack dbStack = dbStackService.getById(dbStackId);
        if (flowLogService.isOtherFlowRunning(dbStackId)) {
            LOGGER.debug("DBStackStatusCheckerJob cannot run, because flow is running for stack: {}", dbStackId);
        } else {
            try {
                measure(() -> dbStackStatusSyncService.sync(dbStack), LOGGER, ":::Auto sync::: DB stack sync in {}ms");
            } catch (Exception e) {
                LOGGER.info(":::Auto sync::: Error occurred during DB sync: {}", e.getMessage(), e);
            }
        }
    }
}
