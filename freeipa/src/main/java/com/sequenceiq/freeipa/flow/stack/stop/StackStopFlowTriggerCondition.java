package com.sequenceiq.freeipa.flow.stack.stop;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class StackStopFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackStopService stackStopService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Long stackId) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.OK;
        Stack stack = stackService.getStackById(stackId);
        if (!stackStopService.isStopPossible(stack)) {
            String msg = "Stopping stack is not possible because stack is not in stop requested state.";
            LOGGER.debug(msg);
            result = new FlowTriggerConditionResult(msg);
        }
        return result;
    }
}
