package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordReason;

public class RotateSaltPasswordContext extends CommonContext {

    private final Stack stack;

    private final RotateSaltPasswordReason reason;

    public RotateSaltPasswordContext(FlowParameters flowParameters, Stack stack, RotateSaltPasswordReason reason) {
        super(flowParameters);
        this.stack = stack;
        this.reason = reason;
    }

    public Stack getStack() {
        return stack;
    }

    public RotateSaltPasswordReason getReason() {
        return reason;
    }
}
