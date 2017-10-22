package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackMinimal;

public class StackFailureContext extends CommonContext {

    private final StackMinimal stackMinimal;

    private final Stack stack;

    public StackFailureContext(String flowId, Stack stack) {
        super(flowId);
        this.stack = stack;
        stackMinimal = null;
    }

    public StackFailureContext(String flowId, StackMinimal stackMinimal) {
        super(flowId);
        stack = null;
        this.stackMinimal = stackMinimal;
    }

    public StackMinimal getStackMinimal() {
        return stackMinimal;
    }

    public Stack getStack() {
        return stack;
    }
}
