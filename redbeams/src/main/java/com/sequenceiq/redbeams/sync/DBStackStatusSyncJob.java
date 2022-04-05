package com.sequenceiq.redbeams.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
public class DBStackStatusSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackStatusSyncJob.class);

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusSyncService dbStackStatusSyncService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public DBStackStatusSyncJob(Tracer tracer) {
        super(tracer, "DB Stack Status Sync Job");
    }

    @Override
    protected Object getMdcContextObject() {
        Long dbStackId = Long.valueOf(getLocalId());
        return dbStackService.getById(dbStackId);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        Long dbStackId = Long.valueOf(getLocalId());
        DBStack dbStack = dbStackService.getById(dbStackId);
        if (flowLogService.isOtherFlowRunning(dbStackId)) {
            LOGGER.debug("DBStackStatusCheckerJob cannot run, because flow is running for stack: {}", dbStackId);
        } else {
            try {
                measure(() -> {
                    ThreadBasedUserCrnProvider.doAsInternalActor(
                            regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            () -> {
                                dbStackStatusSyncService.sync(dbStack);
                            }
                    );
                }, LOGGER, ":::Auto sync::: DB stack sync in {}ms");
            } catch (Exception e) {
                LOGGER.info(":::Auto sync::: Error occurred during DB sync: {}", e.getMessage(), e);
            }
        }
    }
}
