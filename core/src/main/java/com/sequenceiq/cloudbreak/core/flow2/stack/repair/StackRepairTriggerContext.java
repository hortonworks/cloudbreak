package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class StackRepairTriggerContext extends CommonContext {

    private final Stack stack;

    public StackRepairTriggerContext(FlowParameters flowParameters, Stack stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
