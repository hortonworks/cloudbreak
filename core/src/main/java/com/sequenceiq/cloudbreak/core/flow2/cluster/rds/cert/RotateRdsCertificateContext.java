package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RotateRdsCertificateContext extends CommonContext {

    private final Long stackId;

    private final StackView stack;

    private final RotateRdsCertificateType type;

    public RotateRdsCertificateContext(FlowParameters flowParameters, Long stackId, StackView stack, RotateRdsCertificateType type) {
        super(flowParameters);
        this.stackId = stackId;
        this.stack = stack;
        this.type = type;
    }

    public Long getStackId() {
        return stackId;
    }

    public StackView getStack() {
        return stack;
    }

    public RotateRdsCertificateType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "RotateRdsCertificateContext{" +
                "stackId=" + stackId +
                "type=" + type +
                "} " + super.toString();
    }
}
