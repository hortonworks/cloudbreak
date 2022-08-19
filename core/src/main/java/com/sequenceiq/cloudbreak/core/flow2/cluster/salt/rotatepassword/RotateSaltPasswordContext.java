package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.rotatepassword;

import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordType;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RotateSaltPasswordContext extends CommonContext {

    private final StackView stack;

    private final RotateSaltPasswordReason reason;

    private final RotateSaltPasswordType type;

    private final StackStatus previousStackStatus;

    public RotateSaltPasswordContext(FlowParameters flowParameters, StackView stack, StackStatus previousStackStatus,
            RotateSaltPasswordReason reason, RotateSaltPasswordType type) {
        super(flowParameters);
        this.stack = stack;
        this.previousStackStatus = previousStackStatus;
        this.reason = reason;
        this.type = type;
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

    public RotateSaltPasswordType getType() {
        return type;
    }

    public StackStatus getPreviousStackStatus() {
        return previousStackStatus;
    }
}
