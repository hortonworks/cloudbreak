package com.sequenceiq.freeipa.sync.saltstatus;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.Optional;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordReason;
import com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.InterruptSyncingException;

@DisallowConcurrentExecution
@Component
public class StackSaltStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackSaltStatusCheckerJob.class);

    @Inject
    private StackSaltStatusCheckerJobService jobService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private StackService stackService;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(stackService.getStackById(getStackId()));
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        Long stackId = getStackId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        try {
            rotateSaltPasswordService.validateRotateSaltPassword(stack);
            Status status = stack.getStackStatus().getStatus();
            if (status.isDeletionInProgress() || status.isSuccessfullyDeleted()) {
                LOGGER.debug("Stack {} is deleted, unscheduling", stack.getResourceCrn());
                jobService.unschedule(context.getJobDetail().getKey());
            } else if (status.isStopInProgressPhase() || status.isStoppedPhase()) {
                LOGGER.debug("StackSaltStatusCheckerJob cannot run, because stack {} is in stopped", stackId);
            } else if (flowLogService.isOtherFlowRunning(stackId)) {
                LOGGER.debug("StackSaltStatusCheckerJob cannot run, because flow is running for freeipa stack: {}", stackId);
            } else {
                LOGGER.debug("No flows running, trying to sync freeipa salt");
                syncAStack(stack);
            }
        } catch (BadRequestException e) {
            LOGGER.info("StackSaltStatusCheckerJob cannot run, because validation failed for stack {} with message: {}", stackId, e.getMessage());
            jobService.unschedule(context.getJobDetail().getKey());
        } catch (InterruptSyncingException e) {
            LOGGER.info("Syncing salt was interrupted", e);
        }
    }

    private void syncAStack(Stack stack) {
        try {
            checkedMeasure(() -> {
                Optional<RotateSaltPasswordReason> rotateSaltPasswordReason = rotateSaltPasswordService.checkIfSaltPasswordRotationNeeded(stack);
                rotateSaltPasswordReason.ifPresent(reason ->
                        rotateSaltPasswordService.triggerRotateSaltPassword(stack.getEnvironmentCrn(), stack.getAccountId(), reason));
            }, LOGGER, ":::Auto sync::: freeipa stack salt sync in {}ms");
        } catch (Exception e) {
            rotateSaltPasswordService.sendFailureUsageReport(stack.getResourceCrn(), RotateSaltPasswordReason.UNSET, e.getMessage());
            LOGGER.warn(":::Auto sync::: Error occurred during freeipa salt sync: {}", e.getMessage(), e);
        }
    }
}
