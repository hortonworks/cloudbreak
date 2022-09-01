package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordReason;

public class RotateSaltPasswordContext extends CommonContext {

    private final Stack stack;

    private final StackStatus previousStackStatus;

    private final RotateSaltPasswordReason reason;

    private final RotateSaltPasswordType type;

    public RotateSaltPasswordContext(FlowParameters flowParameters, Stack stack, StackStatus previousStackStatus, RotateSaltPasswordReason reason,
            RotateSaltPasswordType type) {
        super(flowParameters);
        this.stack = stack;
        this.previousStackStatus = previousStackStatus;
        this.reason = reason;
        this.type = type;
    }

    public Stack getStack() {
        return stack;
    }

    public StackStatus getPreviousStackStatus() {
        return previousStackStatus;
    }

    public RotateSaltPasswordReason getReason() {
        return reason;
    }

    public RotateSaltPasswordType getType() {
        return type;
    }
}
