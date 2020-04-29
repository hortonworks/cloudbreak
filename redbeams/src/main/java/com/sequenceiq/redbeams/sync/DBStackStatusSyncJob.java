package com.sequenceiq.redbeams.sync;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.statuschecker.job.StatusCheckerJob;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

public class DBStackStatusSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackStatusSyncJob.class);

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusSyncService dbStackStatusSyncService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long dbStackId = Long.valueOf(getLocalId());
        DBStack dbStack = dbStackService.getById(dbStackId);

        MDCBuilder.buildMdcContext(dbStack);

        if (flowLogService.isOtherFlowRunning(dbStackId)) {
            LOGGER.debug("DBStackStatusCheckerJob cannot run, because flow is running for stack: {}", dbStackId);
        } else {
            try {
                measure(() -> {
                    Crn internalCrnForService = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForService();
                    ThreadBasedUserCrnProvider.doAs(internalCrnForService.toString(), () -> {
                        dbStackStatusSyncService.sync(dbStack);
                    });
                }, LOGGER, ":::Auto sync::: DB stack sync in {}ms");
            } catch (Exception e) {
                    LOGGER.info(":::Auto sync::: Error occurred during DB sync: {}", e.getMessage(), e);
            }
        }

        MDCBuilder.cleanupMdc();
    }

}
