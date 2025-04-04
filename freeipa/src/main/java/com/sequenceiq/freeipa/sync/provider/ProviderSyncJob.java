package com.sequenceiq.freeipa.sync.provider;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@DisallowConcurrentExecution
@Component
public class ProviderSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncJob.class);

    @Inject
    private StackService stackService;

    @Inject
    private ProviderSyncService providerSyncService;

    @Inject
    private ProviderSyncJobService jobService;

    @Inject
    private ProviderSyncConfig config;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private InternalCrnModifier internalCrnModifier;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(stackService.getStackById(getStackId()));
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) {
        Long stackId = getStackId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Status status = stack.getStackStatus().getStatus();
        if (!config.isProviderSyncEnabled()) {
            LOGGER.info("ProviderSyncJob cannot run, because feature is disabled.");
        } else if (flowLogService.isOtherFlowRunning(stackId)) {
            LOGGER.info("ProviderSyncJob cannot run, because flow is running for FreeIPA.");
        } else if (status.isUnschedulableState()) {
            LOGGER.info("ProviderSyncJob job will be unscheduled for FreeIPA, stack state is {}", status);
            jobService.unschedule(context.getJobDetail().getKey());
        } else if (status.isAvailable()) {
            LOGGER.info("ProviderSyncJob will run...");
            ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            stack.getAccountId()).toString(), () -> providerSyncService.syncResources(stack));
        } else {
            LOGGER.info("ProviderSyncJob will not run, because FreeIPA stack status is {}.", status);
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}