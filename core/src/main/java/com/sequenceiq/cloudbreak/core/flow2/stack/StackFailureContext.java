package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackView;

public class StackFailureContext extends CommonContext {

    private final StackView stackView;

    private final Stack stack;

    public StackFailureContext(String flowId, Stack stack) {
        super(flowId);
        this.stack = stack;
        stackView = null;
    }

    public StackFailureContext(String flowId, StackView stackView) {
        super(flowId);
        stack = null;
        this.stackView = stackView;
    }

    public StackView getStackView() {
        return stackView;
    }

    public Stack getStack() {
        return stack;
    }
}
