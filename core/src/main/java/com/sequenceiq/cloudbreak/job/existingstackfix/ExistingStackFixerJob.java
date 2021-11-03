package com.sequenceiq.cloudbreak.job.existingstackfix;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackFix.StackFixType;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.existingstackfix.ExistingStackFixService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class ExistingStackFixerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackFixerJob.class);

    @Inject
    private StackViewService stackViewService;

    @Inject
    private StackService stackService;

    @Inject
    private ExistingStackFixerJobService jobService;

    @Inject
    private Collection<ExistingStackFixService> existingStackFixServices;

    public ExistingStackFixerJob(Tracer tracer) {
        super(tracer, "Existing Stack Fixer Job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackViewService.findById(getStackId()).orElseGet(StackView::new);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        Stack stack = stackService.getByIdWithListsInTransaction(getStackId());

        Status stackStatus = stack.getStatus();
        if (failedOrInDeleteStatuses().contains(stackStatus)) {
            LOGGER.debug("Existing stack fixing will be unscheduled, stack {} state is {}", stack.getResourceCrn(), stackStatus);
            jobService.unschedule(getLocalId());
            return;
        }

        Map<StackFixType, Exception> errors = new HashMap<>();
        for (ExistingStackFixService existingStackFixService : existingStackFixServices) {
            StackFixType stackFixType = existingStackFixService.getStackFixType();
            if (existingStackFixService.isStackAlreadyFixed(stack)) {
                LOGGER.debug("Stack {} was already fixed for {}", stack.getResourceCrn(), stackFixType);
                continue;
            }
            try {
                if (!existingStackFixService.isAffected(stack)) {
                    LOGGER.debug("Stack {} is not affected by {}", stack.getResourceCrn(), stackFixType);
                    continue;
                }
                LOGGER.debug("Stack {} needs fix for {}", stack.getResourceCrn(), stackFixType);
                existingStackFixService.apply(stack);
                LOGGER.info("Stack {} was fixed successfully for {}", stack.getResourceCrn(), stackFixType);
            } catch (Exception e) {
                LOGGER.error("Failed to fix stack: {} for {}", stack.getResourceCrn(), stackFixType, e);
                errors.put(stackFixType, e);
            }
        }
        if (!errors.isEmpty()) {
            throw new JobExecutionException(String.format("Failed to fix stack %s, errors: %s", stack.getResourceCrn(), errors));
        }
        LOGGER.info("All fixes finished for stack {}", stack.getResourceCrn());
        jobService.unschedule(getLocalId());
    }

    @VisibleForTesting
    Set<Status> failedOrInDeleteStatuses() {
        return EnumSet.of(
                Status.CREATE_FAILED,
                Status.PRE_DELETE_IN_PROGRESS,
                Status.DELETE_IN_PROGRESS,
                Status.DELETE_FAILED,
                Status.DELETE_COMPLETED,
                Status.EXTERNAL_DATABASE_CREATION_FAILED,
                Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                Status.EXTERNAL_DATABASE_DELETION_FINISHED,
                Status.EXTERNAL_DATABASE_DELETION_FAILED,
                Status.LOAD_BALANCER_UPDATE_FINISHED,
                Status.LOAD_BALANCER_UPDATE_FAILED
        );
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
