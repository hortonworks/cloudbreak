package com.sequenceiq.stackpatcher.job;

import static com.sequenceiq.stackpatcher.job.ExistingStackPatcherJobAdapter.STACK_PATCH_TYPE_NAME;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.domain.StackPatchResource;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.stackpatcher.domain.StackPatch;
import com.sequenceiq.stackpatcher.domain.StackPatchStatus;
import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.domain.StackPatchTypeStatus;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchApplyException;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchService;
import com.sequenceiq.stackpatcher.service.StackPatchResourceService;
import com.sequenceiq.stackpatcher.service.StackPatchService;

@DisallowConcurrentExecution
@Component
public class ExistingStackPatcherJob<R extends StackPatchResource> extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJob.class);

    @Inject
    private StackPatchResourceService<R> stackPatchResourceService;

    @Inject
    private ExistingStackPatcherJobService jobService;

    @Inject
    private ExistingStackPatcherServiceProvider existingStackPatcherServiceProvider;

    @Inject
    private StackPatchService stackPatchService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return stackPatchResourceService.getMdcContextInfoProvider(getLocalIdAsLong());
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws JobExecutionException {
        R resource = stackPatchResourceService.loadResource(getLocalIdAsLong());
        String stackPatchTypeName = context.getJobDetail().getJobDataMap().getString(STACK_PATCH_TYPE_NAME);
        try {
            ExistingStackPatchService<R> existingStackPatchService =
                    (ExistingStackPatchService<R>) existingStackPatcherServiceProvider.provide(stackPatchTypeName);
            StackPatchType stackPatchType = existingStackPatchService.getStackPatchType();
            if (stackPatchResourceService.isUnschedulable(resource)) {
                LOGGER.debug("Existing stack patching will be unscheduled, because stack {} is in an unschedulable state", resource.getResourceCrn());
                stackPatchService.find(resource.getId(), stackPatchType)
                        .ifPresent(stackPatch -> stackPatchService.updateStatus(stackPatch, StackPatchStatus.UNSCHEDULED));
                unscheduleJob(context, stackPatchType, resource);
            } else {
                StackPatch stackPatch = stackPatchService.getOrCreate(resource.getId(), stackPatchType);
                if (StackPatchTypeStatus.DEPRECATED.equals(stackPatchType.getStatus())) {
                    unscheduleJob(context, stackPatchType, resource);
                } else {
                    boolean success = applyStackPatch(existingStackPatchService, stackPatch, resource);
                    if (success) {
                        unscheduleJob(context, stackPatchType, resource);
                    }
                }
            }
        } catch (UnknownStackPatchTypeException e) {
            String message = "Unknown stack patch type: " + stackPatchTypeName;
            unscheduleAndFailJob(message, context, new StackPatch(resource.getId(), StackPatchType.UNKNOWN), resource);
        } catch (Exception e) {
            LOGGER.error("Failed", e);
            throw e;
        }
    }

    private void unscheduleAndFailJob(String message, JobExecutionContext context, StackPatch stackPatch, R resource) throws JobExecutionException {
        LOGGER.info("Unscheduling and failing stack patcher {} for stack {} with message: {}",
                stackPatch.getType(), resource.getResourceCrn(), message);
        unscheduleJob(context, stackPatch.getType(), resource);
        stackPatchService.updateStatusAndNotify(stackPatch, resource, StackPatchStatus.FAILED, message);
        throw new JobExecutionException(message);
    }

    private void unscheduleJob(JobExecutionContext context, StackPatchType stackPatchType, R resource) {
        LOGGER.info("Unscheduling stack patcher {} job for stack {}", stackPatchType, resource.getResourceCrn());
        jobService.unschedule(context.getJobDetail().getKey());
    }

    private boolean applyStackPatch(ExistingStackPatchService<R> existingStackPatchService, StackPatch stackPatch, R resource) throws JobExecutionException {
        StackPatchType stackPatchType = existingStackPatchService.getStackPatchType();
        if (StackPatchStatus.FIXED.equals(stackPatch.getStatus())) {
            LOGGER.debug("Stack {} was already patched for {}", resource.getResourceCrn(), stackPatchType);
            return true;
        }
        try {
            if (!existingStackPatchService.isAffected(resource)) {
                LOGGER.debug("Stack {} is not affected by {}", resource.getResourceCrn(), stackPatchType);
                stackPatchService.updateStatus(stackPatch, StackPatchStatus.NOT_AFFECTED);
                return true;
            }
            LOGGER.debug("Stack {} needs patch for {}", resource.getResourceCrn(), stackPatchType);
            if (!existingStackPatchService.isEntitled(resource)) {
                LOGGER.debug("Stack {} is not entitled for patch {}", resource.getResourceCrn(), stackPatchType);
                stackPatchService.updateStatus(stackPatch, StackPatchStatus.SKIPPED);
                return false;
            }
            if (StackPatchStatus.SCHEDULED.equals(stackPatch.getStatus())) {
                // only report AFFECTED status once
                stackPatchService.updateStatusAndNotify(stackPatch, resource, StackPatchStatus.AFFECTED);
            } else {
                stackPatchService.updateStatus(stackPatch, StackPatchStatus.AFFECTED);
            }
            boolean success = existingStackPatchService.apply(resource);
            if (success) {
                stackPatchService.updateStatusAndNotify(stackPatch, resource, StackPatchStatus.FIXED);
            } else {
                stackPatchService.updateStatus(stackPatch, StackPatchStatus.SKIPPED);
            }
            return success;
        } catch (ExistingStackPatchApplyException e) {
            String message = String.format("Failed to patch stack %s for %s", resource.getResourceCrn(), stackPatchType);
            LOGGER.error(message, e);
            stackPatchService.updateStatusAndNotify(stackPatch, resource, StackPatchStatus.FAILED, e.getMessage());
            throw new JobExecutionException(message, e);
        }
    }
}
