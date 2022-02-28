package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.repository.StackPatchRepository;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowRetryService;

public abstract class ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatchService.class);

    @Inject
    private StackPatchRepository stackPatchRepository;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowRetryService flowRetryService;

    public boolean isStackAlreadyFixed(Stack stack) {
        return stackPatchRepository.findByStackAndType(stack, getStackPatchType()).isPresent();
    }

    public void apply(Stack stack) throws ExistingStackPatchApplyException {
        if (flowLogService.isOtherFlowRunning(stack.getId())) {
            String message = String.format("Another flow is running for stack %s, skipping patch apply to let the flow finish", stack.getResourceCrn());
            throw new ExistingStackPatchApplyException(message);
        } else {
            Optional<FlowLog> lastRetryableFailedFlow = flowRetryService.getLastRetryableFailedFlow(stack.getId());
            if (lastRetryableFailedFlow.isEmpty()) {
                try {
                    LOGGER.info("Starting stack {} patching for {}", stack.getResourceCrn(), getStackPatchType());
                    checkedMeasure(() -> doApply(stack), LOGGER, "Existing stack patching took {} ms for stack {} and patch {}.",
                            stack.getResourceCrn(), getStackPatchType());
                    LOGGER.info("Stack {} was patched successfully for {}", stack.getResourceCrn(), getStackPatchType());
                    stackPatchRepository.save(new StackPatch(stack, getStackPatchType()));
                } catch (ExistingStackPatchApplyException e) {
                    throw e;
                } catch (Exception e) {
                    String message = String.format("Something unexpected went wrong with stack %s while applying patch %s",
                            stack.getResourceCrn(), getStackPatchType());
                    throw new ExistingStackPatchApplyException(message, e);
                }
            } else {
                String message = String.format("Stack %s has a retryable failed flow, skipping patch apply to preserve possible retry", stack.getResourceCrn());
                throw new ExistingStackPatchApplyException(message);
            }
        }
    }

    protected boolean isCmServerReachable(Stack stack) throws ExistingStackPatchApplyException {
        return stack.getClusterManagerServer()
                .orElseThrow(() -> new ExistingStackPatchApplyException("Could not find CM server for stack: " + stack.getResourceCrn()))
                .isReachable();
    }

    protected boolean isPrimaryGatewayReachable(Stack stack) throws ExistingStackPatchApplyException {
        return Optional.ofNullable(stack.getPrimaryGatewayInstance())
                .orElseThrow(() -> new ExistingStackPatchApplyException("Could not find Primary gateway for stack: " + stack.getResourceCrn()))
                .isReachable();
    }

    /**
     * @return The StackPatchType that is patched by running the service implementation's doApply
     */
    public abstract StackPatchType getStackPatchType();

    /**
     * @return Is the stack affected by the {@link StackPatchType}
     */
    public abstract boolean isAffected(Stack stack);

    /**
     * Apply the fix of {@link StackPatchType} for the affected stack
     */
    abstract void doApply(Stack stack) throws ExistingStackPatchApplyException;
}
