package com.sequenceiq.freeipa.flow.stack;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;

public class StackFailureContext extends CommonContext {

    private final Stack stack;

    public StackFailureContext(FlowParameters flowParameters, Stack stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }

}
