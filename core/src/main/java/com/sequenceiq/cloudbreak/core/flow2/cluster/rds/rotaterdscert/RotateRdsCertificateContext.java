package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RotateRdsCertificateContext extends CommonContext {

    private final Long stackId;

    private final StackView stack;

    public RotateRdsCertificateContext(FlowParameters flowParameters, Long stackId, StackView stack) {
        super(flowParameters);
        this.stackId = stackId;
        this.stack = stack;
    }

    public Long getStackId() {
        return stackId;
    }

    public StackView getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return "RotateRdsCertificateContext{" +
                "stackId=" + stackId +
                "} " + super.toString();
    }
}
