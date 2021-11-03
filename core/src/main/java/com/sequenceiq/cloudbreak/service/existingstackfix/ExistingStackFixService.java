package com.sequenceiq.cloudbreak.service.existingstackfix;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackFix;
import com.sequenceiq.cloudbreak.domain.stack.StackFix.StackFixType;
import com.sequenceiq.cloudbreak.repository.StackFixRepository;
import com.sequenceiq.flow.core.FlowLogService;

public abstract class ExistingStackFixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackFixService.class);

    @Inject
    private StackFixRepository stackFixRepository;

    @Inject
    private FlowLogService flowLogService;

    public boolean isStackAlreadyFixed(Stack stack) {
        return stackFixRepository.findByStackAndType(stack, getStackFixType()).isPresent();
    }

    public void apply(Stack stack) {
        if (flowLogService.isOtherFlowRunning(stack.getId())) {
            throw new IllegalStateException("Another flow is running for stack " + stack.getResourceCrn());
        }

        measure(() -> doApply(stack), LOGGER, "Existing stack fixing {} took {} ms for stack: {}.", getStackFixType(), stack.getResourceCrn());

        LOGGER.info("Stack {} was fixed successfully for {}", stack.getResourceCrn(), getStackFixType());
        stackFixRepository.save(new StackFix(stack, getStackFixType()));
    }

    /**
     * @return The StackFixType that is fixed by running the service implementation's doApply
     */
    public abstract StackFixType getStackFixType();

    /**
     * @return Is the stack affected by the {@link StackFixType}
     */
    public abstract boolean isAffected(Stack stack);

    /**
     * Apply the fix of {@link StackFixType} for the affected stack
     */
    abstract void doApply(Stack stack);
}
