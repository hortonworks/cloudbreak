package com.sequenceiq.stackpatcher.service;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.domain.StackPatchResource;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowRetryService;
import com.sequenceiq.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.stackpatcher.domain.StackPatchType;

public abstract class ExistingStackPatchService<R extends StackPatchResource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatchService.class);

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowRetryService flowRetryService;

    @Inject
    private ExistingStackPatcherConfig properties;

    @Inject
    private StackPatchEntitlementService stackPatchEntitlementService;

    public int getIntervalInMinutes() {
        return (int) TimeUnit.HOURS.toMinutes(properties.getIntervalInHours());
    }

    /**
     * @param resource the resource to apply the patch for
     * @return whether the patch was applied successfully
     * @throws ExistingStackPatchApplyException when something unexpected goes wrong while applying the patch
     */
    public boolean apply(R resource) throws ExistingStackPatchApplyException {
        if (flowLogService.isOtherFlowRunning(resource.getId())) {
            LOGGER.info("Another flow is running for stack {}, skipping patch apply to let the flow finish", resource.getResourceCrn());
            return false;
        }
        if (shouldCheckForFailedRetryableFlow() && hasRetryableFailedFlow(resource)) {
            LOGGER.info("Stack {} has a retryable failed flow, skipping patch apply to preserve possible retry", resource.getResourceCrn());
            return false;
        }
        try {
            LOGGER.info("Starting stack {} patching for {}", resource.getResourceCrn(), getStackPatchType());
            boolean success = checkedMeasure(() -> doApply(resource), LOGGER, "Existing stack patching took {} ms for stack {} and patch {}.",
                    resource.getResourceCrn(), getStackPatchType());
            if (success) {
                LOGGER.info("Stack {} was patched successfully for {}", resource.getResourceCrn(), getStackPatchType());
            } else {
                LOGGER.info("Stack {} was not patched for {}", resource.getResourceCrn(), getStackPatchType());
            }
            return success;
        } catch (ExistingStackPatchApplyException e) {
            throw e;
        } catch (Exception e) {
            String message = String.format("Something unexpected went wrong with stack %s while applying patch %s: %s",
                    resource.getResourceCrn(), getStackPatchType(), e.getMessage());
            throw new ExistingStackPatchApplyException(message, e);
        }
    }

    private boolean hasRetryableFailedFlow(R resource) {
        Optional<FlowLog> lastRetryableFailedFlow = flowRetryService.getLastRetryableFailedFlow(resource.getId());
        return lastRetryableFailedFlow.isPresent();
    }

    /**
     * Override this to false when the stack patcher does not start a flow, or it should start its flow either way
     */
    protected boolean shouldCheckForFailedRetryableFlow() {
        return true;
    }

    /**
     * @return The StackPatchType that is patched by running the service implementation's doApply
     */
    public abstract StackPatchType getStackPatchType();

    /**
     * @return Is the resource affected by the {@link StackPatchType}
     */
    public abstract boolean isAffected(R resource);

    /**
     * @return Whether the resource is entitled for the {@link StackPatchType}
     */
    public boolean isEntitled(R resource) {
        return stackPatchEntitlementService.isEntitled(resource, getStackPatchType());
    }

    /**
     * Apply the fix of {@link StackPatchType} for the affected resource
     *
     * @param resource the resource to apply the patch for
     * @return whether the patch was applied successful
     * @throws ExistingStackPatchApplyException when something unexpected goes wrong while applying the patch
     */
    protected abstract boolean doApply(R resource) throws ExistingStackPatchApplyException;
}
