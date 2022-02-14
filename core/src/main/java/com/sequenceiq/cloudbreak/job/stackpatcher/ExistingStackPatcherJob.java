package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.job.stackpatcher.ExistingStackPatcherJobAdapter.STACK_PATCH_TYPE_NAME;

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.converter.StackPatchTypeConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchApplyException;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchUsageReporterService;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class ExistingStackPatcherJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJob.class);

    @Inject
    private StackViewService stackViewService;

    @Inject
    private StackService stackService;

    @Inject
    private StackPatchTypeConverter stackPatchTypeConverter;

    @Inject
    private ExistingStackPatcherJobService jobService;

    @Inject
    private Collection<ExistingStackPatchService> existingStackPatchServices;

    @Inject
    private StackPatchUsageReporterService stackPatchUsageReporterService;

    public ExistingStackPatcherJob(Tracer tracer) {
        super(tracer, "Existing Stack Patcher Job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackViewService.findById(getStackId()).orElseGet(StackView::new);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        Stack stack = stackService.getByIdWithListsInTransaction(getStackId());
        Status stackStatus = stack.getStatus();
        if (!Status.getUnschedulableStatuses().contains(stackStatus)) {
            String stackPatchTypeName = context.getJobDetail().getJobDataMap().getString(STACK_PATCH_TYPE_NAME);
            StackPatchType stackPatchType = stackPatchTypeConverter.convertToEntityAttribute(stackPatchTypeName);
            if (stackPatchType == null || StackPatchType.UNKNOWN.equals(stackPatchType)) {
                String message = String.format("Stack patch type %s is unknown", stackPatchTypeName);
                unscheduleAndFail(message, context, stack, stackPatchType);
            } else {
                Optional<ExistingStackPatchService> optionalExistingStackPatchService = getStackPatchServiceForType(stackPatchType);
                if (optionalExistingStackPatchService.isEmpty()) {
                    String message = "No stack patcher implementation found for type " + stackPatchType;
                    unscheduleAndFail(message, context, stack, stackPatchType);
                } else {
                    applyStackPatch(optionalExistingStackPatchService.get(), stack);
                }
            }
        } else {
            LOGGER.debug("Existing stack patching will be unscheduled, because stack {} status is {}", stack.getResourceCrn(), stackStatus);
        }
        unscheduleJob(context);
    }

    private void unscheduleAndFail(String message, JobExecutionContext context, Stack stack, StackPatchType stackPatchType)
            throws JobExecutionException {
        unscheduleJob(context);
        stackPatchUsageReporterService.reportFailure(stack, stackPatchType, message);
        throw new JobExecutionException(message);
    }

    private void unscheduleJob(JobExecutionContext context) {
        jobService.unschedule(context.getJobDetail().getKey());
    }

    private Optional<ExistingStackPatchService> getStackPatchServiceForType(StackPatchType stackPatchType) {
        return existingStackPatchServices.stream()
                .filter(existingStackPatchService -> stackPatchType.equals(existingStackPatchService.getStackPatchType()))
                .findFirst();
    }

    private void applyStackPatch(ExistingStackPatchService existingStackPatchService, Stack stack) throws JobExecutionException {
        StackPatchType stackPatchType = existingStackPatchService.getStackPatchType();
        if (!existingStackPatchService.isStackAlreadyFixed(stack)) {
            try {
                if (existingStackPatchService.isAffected(stack)) {
                    LOGGER.debug("Stack {} needs patch for {}", stack.getResourceCrn(), stackPatchType);
                    stackPatchUsageReporterService.reportAffected(stack, stackPatchType);
                    existingStackPatchService.apply(stack);
                    LOGGER.info("Stack {} was patched successfully for {}", stack.getResourceCrn(), stackPatchType);
                    stackPatchUsageReporterService.reportSuccess(stack, stackPatchType);
                } else {
                    LOGGER.debug("Stack {} is not affected by {}", stack.getResourceCrn(), stackPatchType);
                }
            } catch (ExistingStackPatchApplyException e) {
                String message = String.format("Failed to patch stack %s for %s", stack.getResourceCrn(), stackPatchType);
                LOGGER.error(message, e);
                stackPatchUsageReporterService.reportFailure(stack, stackPatchType, e.getMessage());
                throw new JobExecutionException(message, e);
            }
        } else {
            LOGGER.debug("Stack {} was already patched for {}", stack.getResourceCrn(), stackPatchType);
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
