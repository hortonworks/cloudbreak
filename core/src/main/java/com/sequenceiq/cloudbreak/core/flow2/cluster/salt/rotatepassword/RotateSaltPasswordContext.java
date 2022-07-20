package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.rotatepassword;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RotateSaltPasswordContext extends CommonContext {

    private final StackView stack;

    private final RotateSaltPasswordReason reason;

    public RotateSaltPasswordContext(FlowParameters flowParameters, StackView stack, RotateSaltPasswordReason reason) {
        super(flowParameters);
        this.stack = stack;
        this.reason = reason;
    }

    public StackView getStack() {
        return stack;
    }

    public Long getStackId() {
        return stack.getId();
    }

    public RotateSaltPasswordReason getReason() {
        return reason;
    }
}
