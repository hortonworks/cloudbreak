package com.sequenceiq.freeipa.flow.stack.modify.proxy;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;

public class ModifyProxyConfigContext extends CommonContext {

    private final Stack stack;

    public ModifyProxyConfigContext(FlowParameters flowParameters, Stack stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
