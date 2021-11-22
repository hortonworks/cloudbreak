package com.sequenceiq.cloudbreak.job.stackpatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchApplyException;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;

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
    private ExistingStackPatcherJobService jobService;

    @Inject
    private Collection<ExistingStackPatchService> existingStackPatchServices;

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
            applyStackPatches(stack);
        } else {
            LOGGER.debug("Existing stack fixing will be unscheduled, because stack {} state is {}", stack.getResourceCrn(), stackStatus);
        }
        jobService.unschedule(context.getJobDetail().getKey());
    }

    private void applyStackPatches(Stack stack) throws JobExecutionException {
        Map<StackPatchType, Exception> errors = new HashMap<>();
        for (ExistingStackPatchService existingStackPatchService : existingStackPatchServices) {
            if (!existingStackPatchService.isStackAlreadyFixed(stack)) {
                try {
                    applyStackPatch(stack, existingStackPatchService);
                } catch (ExistingStackPatchApplyException e) {
                    LOGGER.error("Failed to patch stack: {} for {}", stack.getResourceCrn(), existingStackPatchService.getStackFixType(), e);
                    errors.put(existingStackPatchService.getStackFixType(), e);
                }
            } else {
                LOGGER.debug("Stack {} was already patched for {}", stack.getResourceCrn(), existingStackPatchService.getStackFixType());
            }
        }
        if (errors.isEmpty()) {
            LOGGER.info("All patches finished for stack {}", stack.getResourceCrn());
        } else {
            throw new JobExecutionException(String.format("Failed to patch stack %s, errors: %s", stack.getResourceCrn(), errors));
        }
    }

    private void applyStackPatch(Stack stack, ExistingStackPatchService existingStackPatchService) throws ExistingStackPatchApplyException {
        StackPatchType stackPatchType = existingStackPatchService.getStackFixType();
        if (existingStackPatchService.isAffected(stack)) {
            LOGGER.debug("Stack {} needs patch for {}", stack.getResourceCrn(), stackPatchType);
            existingStackPatchService.apply(stack);
            LOGGER.info("Stack {} was patched successfully for {}", stack.getResourceCrn(), stackPatchType);
        } else {
            LOGGER.debug("Stack {} is not affected by {}", stack.getResourceCrn(), stackPatchType);
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
