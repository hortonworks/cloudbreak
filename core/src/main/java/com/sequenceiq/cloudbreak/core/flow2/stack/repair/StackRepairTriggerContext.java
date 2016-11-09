package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

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
