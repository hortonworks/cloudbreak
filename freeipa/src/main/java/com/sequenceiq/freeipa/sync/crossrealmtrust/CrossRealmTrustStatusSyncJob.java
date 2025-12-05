package com.sequenceiq.freeipa.sync.crossrealmtrust;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.provider.ProviderSyncJobService;

@DisallowConcurrentExecution
@Component
public class CrossRealmTrustStatusSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossRealmTrustStatusSyncJob.class);

    @Inject
    private StackService stackService;

    @Inject
    private CrossRealmTrustStatusSyncService crossRealmTrustStatusSyncService;

    @Inject
    private ProviderSyncJobService jobService;

    @Inject
    private CrossRealmTrustStatusSyncConfig config;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(stackService.getStackById(getStackId()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        Long stackId = getStackId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Status status = stack.getStackStatus().getStatus();
        if (!config.isEnabled()) {
            LOGGER.info("CrossRealmTrustStatusSyncJob cannot run, because feature is disabled.");
        } else if (flowLogService.isOtherFlowRunning(stackId)) {
            LOGGER.info("CrossRealmTrustStatusSyncJob cannot run, because flow is running for FreeIPA.");
        } else if (status.isUnschedulableState()) {
            LOGGER.info("CrossRealmTrustStatusSyncJob job will be unscheduled for FreeIPA, stack state is {}", status);
            jobService.deregister(context.getJobDetail().getKey());
        } else if (status.isAvailable()) {
            Optional<CrossRealmTrust> crossRealmTrust = crossRealmTrustService.getByStackIdIfExists(stackId);
            if (crossRealmTrust.isEmpty()) {
                LOGGER.info("CrossRealmTrustStatusSyncJob will not run, because cross realm trust is not found.");
                jobService.deregister(context.getJobDetail().getKey());
            } else if (!TrustStatus.SYNC_ENABLED_STATUSES.contains(crossRealmTrust.get().getTrustStatus())) {
                LOGGER.info("CrossRealmTrustStatusSyncJob will not run, because cross realm trust status is {}.", crossRealmTrust.get().getTrustStatus());
            } else {
                LOGGER.info("CrossRealmTrustStatusSyncJob will run...");
                crossRealmTrustStatusSyncService.syncCrossRealmTrustStatus(stack, crossRealmTrust.get());
            }
        } else {
            LOGGER.info("CrossRealmTrustStatusSyncJob will not run, because FreeIPA stack status is {}.", status);
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
