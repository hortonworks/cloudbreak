package com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class UpdateSubnetContext extends CommonContext {
    private Stack stack;

    public UpdateSubnetContext(String flowId, Stack stack) {
        super(flowId);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
