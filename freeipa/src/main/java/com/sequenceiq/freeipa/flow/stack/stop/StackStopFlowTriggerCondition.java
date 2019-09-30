package com.sequenceiq.freeipa.flow.stack.stop;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class StackStopFlowTriggerCondition implements FlowTriggerCondition {
    @Inject
    private StackService stackService;

    @Inject
    private StackStopService stackStopService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        return stackStopService.isStopPossible(stack);
    }
}
