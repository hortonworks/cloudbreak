package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;

public class ChangePrimaryGatewayContext extends CommonContext {

    private final Stack stack;

    public ChangePrimaryGatewayContext(FlowParameters flowParameters, Stack stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
