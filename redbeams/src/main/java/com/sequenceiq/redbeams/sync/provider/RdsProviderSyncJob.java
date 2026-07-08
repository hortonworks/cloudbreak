package com.sequenceiq.redbeams.sync.provider;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@DisallowConcurrentExecution
@Component
public class RdsProviderSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsProviderSyncJob.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private RdsProviderSyncService rdsProviderSyncService;

    @Inject
    private RdsProviderSyncJobService jobService;

    @Inject
    private RdsProviderSyncConfig config;

    @Inject
    private FlowLogService flowLogService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(dbStackService.getById(getDbStackId()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        Long dbStackId = getDbStackId();
        DBStack dbStack = dbStackService.getById(dbStackId);
        Status status = dbStack.getStatus();
        if (!config.isEnabled()) {
            LOGGER.info(":::RDS provider sync::: cannot run, because feature is disabled.");
        } else if (flowLogService.isOtherFlowRunning(dbStackId)) {
            LOGGER.info(":::RDS provider sync::: cannot run, because flow is running for DB stack {}.", dbStackId);
        } else if (status.isDeleteCompleted()) {
            LOGGER.info(":::RDS provider sync::: job will be unscheduled for DB stack {}, status is {}.", dbStackId, status);
            jobService.deregister(context.getJobDetail().getKey());
        } else if (status.isAvailable()) {
            LOGGER.info(":::RDS provider sync::: will run for DB stack {}...", dbStackId);
            rdsProviderSyncService.syncInstanceTypeAndVersion(dbStack);
        } else {
            LOGGER.info(":::RDS provider sync::: will not run, because DB stack {} status is {}.", dbStackId, status);
        }
    }

    private Long getDbStackId() {
        return Long.valueOf(getLocalId());
    }
}
