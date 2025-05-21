package com.sequenceiq.cloudbreak.job.salt;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.EnumSet;
import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordService;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordTriggerService;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordValidator;
import com.sequenceiq.cloudbreak.service.salt.SaltPasswordStatusService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@DisallowConcurrentExecution
@Component
public class StackSaltStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackSaltStatusCheckerJob.class);

    private static final EnumSet<Status> IGNORED_STATES = EnumSet.of(
            Status.REQUESTED,
            Status.CREATE_IN_PROGRESS,
            Status.UPDATE_IN_PROGRESS,
            Status.UPDATE_REQUESTED,
            Status.STOP_REQUESTED,
            Status.START_REQUESTED,
            Status.STOP_IN_PROGRESS,
            Status.START_IN_PROGRESS,
            Status.WAIT_FOR_SYNC,
            Status.MAINTENANCE_MODE_ENABLED,
            Status.EXTERNAL_DATABASE_CREATION_IN_PROGRESS,
            Status.BACKUP_IN_PROGRESS,
            Status.RESTORE_IN_PROGRESS,
            Status.LOAD_BALANCER_UPDATE_IN_PROGRESS,
            Status.RECOVERY_IN_PROGRESS,
            Status.RECOVERY_REQUESTED
    );

    private static final EnumSet<Status> SYNCABLE_STATES = EnumSet.of(
            Status.AVAILABLE,
            Status.UPDATE_FAILED,
            Status.ENABLE_SECURITY_FAILED,
            Status.START_FAILED,
            Status.STOP_FAILED,
            Status.AMBIGUOUS,
            Status.UNREACHABLE,
            Status.NODE_FAILURE,
            Status.RESTORE_FAILED,
            Status.BACKUP_FAILED,
            Status.BACKUP_FINISHED,
            Status.RESTORE_FINISHED,
            Status.EXTERNAL_DATABASE_START_FAILED,
            Status.EXTERNAL_DATABASE_START_IN_PROGRESS,
            Status.EXTERNAL_DATABASE_START_FINISHED,
            Status.EXTERNAL_DATABASE_STOP_FAILED,
            Status.EXTERNAL_DATABASE_STOP_IN_PROGRESS,
            Status.EXTERNAL_DATABASE_STOP_FINISHED,
            Status.RECOVERY_FAILED,
            Status.UPGRADE_CCM_FAILED,
            Status.UPGRADE_CCM_FINISHED,
            Status.UPGRADE_CCM_IN_PROGRESS
    );

    @Inject
    private StackSaltStatusCheckerJobService jobService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Inject
    private RotateSaltPasswordTriggerService rotateSaltPasswordTriggerService;

    @Inject
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    @Inject
    private SaltPasswordStatusService saltPasswordStatusService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return stackDtoService.getStackViewByIdOpt(getStackId()).map(MdcContextInfoProvider.class::cast);
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws JobExecutionException {
        try {
            measure(() -> {
                Optional<StackDto> stackOptional = stackDtoService.getByIdOpt(getStackId());
                if (stackOptional.isEmpty()) {
                    LOGGER.debug("Stack salt sync will be unscheduled, stack with id {} is not found", getStackId());
                    jobService.unschedule(context.getJobDetail().getKey());
                } else {
                    StackDto stack = stackOptional.get();
                    Status stackStatus = stack.getStatus();
                    if (Status.getUnschedulableStatuses().contains(stackStatus)) {
                        LOGGER.debug("Stack salt sync will be unscheduled, stack state is {}", stackStatus);
                        jobService.unschedule(context.getJobDetail().getKey());
                    } else if (null == stackStatus || IGNORED_STATES.contains(stackStatus)) {
                        LOGGER.debug("Stack salt sync is skipped, stack state is {}", stackStatus);
                    } else if (SYNCABLE_STATES.contains(stackStatus)) {
                        rotateSaltPasswordValidator.validateRotateSaltPassword(stack);
                        rotateSaltPasswordIfNeeded(stack, context);
                    } else {
                        LOGGER.warn("Unhandled stack status, {}", stackStatus);
                    }
                }
            }, LOGGER, "Check salt status took {} ms for stack {}.", getStackId());
        } catch (BadRequestException e) {
            LOGGER.info("StackSaltStatusCheckerJob cannot run, because validation failed for stack {} with message: {}", getStackId(), e.getMessage());
            jobService.unschedule(context.getJobDetail().getKey());
        } catch (Exception e) {
            LOGGER.info("Exception during stack salt status check.", e);
        }
    }

    private void rotateSaltPasswordIfNeeded(StackDto stack, JobExecutionContext context) {
        try {
            SaltPasswordStatus status = saltPasswordStatusService.getSaltPasswordStatus(stack);
            Optional<RotateSaltPasswordReason> reasonOptional = RotateSaltPasswordReason.getForStatus(status);
            if (reasonOptional.isPresent()) {
                RotateSaltPasswordReason reason = reasonOptional.get();
                LOGGER.info("Triggering salt password rotation for status {} with reason {}", status, reason);
                if (rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)) {
                    rotateSaltPasswordTriggerService.triggerRotateSaltPassword(stack, reason);
                } else {
                    LOGGER.warn("Only fallback mechanism is supported for salt password rotation, which might require manual intervention, " +
                            "we suggest to initiate the rotation manually, skipping automated rotation!");
                    jobService.unschedule(context.getJobDetail().getKey());
                }
            } else {
                LOGGER.debug("Salt password rotation is not needed for status {}", status);
            }
        } catch (Exception e) {
            String message = "Failed to get salt password status: " + e.getMessage();
            rotateSaltPasswordService.sendFailureUsageReport(stack.getResourceCrn(), RotateSaltPasswordReason.UNSET, message);
            throw e;
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
