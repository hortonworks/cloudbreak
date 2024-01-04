package com.sequenceiq.freeipa.flow.stack.stop;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
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
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.ok();
        Stack stack = stackService.getStackById(payload.getResourceId());
        if (!stackStopService.isStopPossible(stack)) {
            String msg = "Stopping stack is not possible because stack is not in stop requested state.";
            LOGGER.debug(msg);
            result = FlowTriggerConditionResult.fail(msg);
        }
        return result;
    }
}
