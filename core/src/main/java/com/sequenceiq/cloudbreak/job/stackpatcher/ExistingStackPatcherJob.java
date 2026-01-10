package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.job.stackpatcher.ExistingStackPatcherJobAdapter.STACK_PATCH_TYPE_NAME;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchTypeStatus;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchApplyException;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchService;

@DisallowConcurrentExecution
@Component
public class ExistingStackPatcherJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJob.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private ExistingStackPatcherJobService jobService;

    @Inject
    private ExistingStackPatcherServiceProvider existingStackPatcherServiceProvider;

    @Inject
    private StackPatchService stackPatchService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getStackId()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws JobExecutionException {
        Stack stack = stackService.getByIdWithListsInTransaction(getStackId());
        Status stackStatus = stack.getStatus();
        String stackPatchTypeName = context.getJobDetail().getJobDataMap().getString(STACK_PATCH_TYPE_NAME);
        try {
            ExistingStackPatchService existingStackPatchService = existingStackPatcherServiceProvider.provide(stackPatchTypeName);
            StackPatchType stackPatchType = existingStackPatchService.getStackPatchType();
            StackPatch stackPatch = stackPatchService.getOrCreate(stack, stackPatchType);
            if (StackPatchTypeStatus.DEPRECATED.equals(stackPatchType.getStatus())) {
                unscheduleJob(context, stackPatch);
            } else {
                if (!Status.getUnschedulableStatuses().contains(stackStatus)) {
                    boolean success = applyStackPatch(existingStackPatchService, stackPatch);
                    if (success) {
                        unscheduleJob(context, stackPatch);
                    }
                } else {
                    LOGGER.debug("Existing stack patching will be unscheduled, because stack {} status is {}", stack.getResourceCrn(), stackStatus);
                    stackPatchService.updateStatus(stackPatch, StackPatchStatus.UNSCHEDULED);
                    unscheduleJob(context, stackPatch);
                }
            }
        } catch (UnknownStackPatchTypeException e) {
            String message = "Unknown stack patch type: " + stackPatchTypeName;
            unscheduleAndFailJob(message, context, new StackPatch(stack, StackPatchType.UNKNOWN));
        } catch (Exception e) {
            LOGGER.error("Failed", e);
            throw e;
        }
    }

    private void unscheduleAndFailJob(String message, JobExecutionContext context, StackPatch stackPatch)
            throws JobExecutionException {
        LOGGER.info("Unscheduling and failing stack patcher {} for stack {} with message: {}",
                stackPatch.getType(), stackPatch.getStack().getResourceCrn(), message);
        unscheduleJob(context, stackPatch);
        stackPatchService.updateStatusAndReportUsage(stackPatch, StackPatchStatus.FAILED, message);
        throw new JobExecutionException(message);
    }

    private void unscheduleJob(JobExecutionContext context, StackPatch stackPatch) {
        LOGGER.info("Unscheduling stack patcher {} job for stack {}", stackPatch.getType(), stackPatch.getStack().getResourceCrn());
        jobService.unschedule(context.getJobDetail().getKey());
    }

    private boolean applyStackPatch(ExistingStackPatchService existingStackPatchService, StackPatch stackPatch) throws JobExecutionException {
        Stack stack = stackPatch.getStack();
        StackPatchType stackPatchType = existingStackPatchService.getStackPatchType();
        if (!StackPatchStatus.FIXED.equals(stackPatch.getStatus())) {
            try {
                if (existingStackPatchService.isAffected(stack)) {
                    LOGGER.debug("Stack {} needs patch for {}", stack.getResourceCrn(), stackPatchType);
                    if (StackPatchStatus.SCHEDULED.equals(stackPatch.getStatus())) {
                        // only report AFFECTED status once
                        stackPatchService.updateStatusAndReportUsage(stackPatch, StackPatchStatus.AFFECTED);
                    } else {
                        stackPatchService.updateStatus(stackPatch, StackPatchStatus.AFFECTED);
                    }
                    boolean success = existingStackPatchService.apply(stack);
                    if (success) {
                        stackPatchService.updateStatusAndReportUsage(stackPatch, StackPatchStatus.FIXED);
                    } else {
                        stackPatchService.updateStatus(stackPatch, StackPatchStatus.SKIPPED);
                    }
                    return success;
                } else {
                    LOGGER.debug("Stack {} is not affected by {}", stack.getResourceCrn(), stackPatchType);
                    stackPatchService.updateStatus(stackPatch, StackPatchStatus.NOT_AFFECTED);
                    return true;
                }
            } catch (ExistingStackPatchApplyException e) {
                String message = String.format("Failed to patch stack %s for %s", stack.getResourceCrn(), stackPatchType);
                LOGGER.error(message, e);
                stackPatchService.updateStatusAndReportUsage(stackPatch, StackPatchStatus.FAILED, e.getMessage());
                throw new JobExecutionException(message, e);
            }
        } else {
            LOGGER.debug("Stack {} was already patched for {}", stack.getResourceCrn(), stackPatchType);
            return true;
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
