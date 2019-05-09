package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class StackRepairTriggerContext extends CommonContext {

    private final Stack stack;

    public StackRepairTriggerContext(String flowId, Stack stack) {
        super(flowId);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
