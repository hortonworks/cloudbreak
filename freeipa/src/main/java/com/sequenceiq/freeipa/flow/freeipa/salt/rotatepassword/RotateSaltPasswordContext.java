package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordReason;

public class RotateSaltPasswordContext extends CommonContext {

    private final Stack stack;

    private final RotateSaltPasswordReason reason;

    private final StackStatus previousStackStatus;

    public RotateSaltPasswordContext(FlowParameters flowParameters, Stack stack, StackStatus previousStackStatus, RotateSaltPasswordReason reason) {
        super(flowParameters);
        this.stack = stack;
        this.previousStackStatus = previousStackStatus;
        this.reason = reason;
    }

    public Stack getStack() {
        return stack;
    }

    public RotateSaltPasswordReason getReason() {
        return reason;
    }

    public StackStatus getPreviousStackStatus() {
        return previousStackStatus;
    }
}
