package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class StackFailureContext extends CommonContext {

    private final StackView stack;

    private final Long stackId;

    private final ProvisionType provisionType;

    public StackFailureContext(FlowParameters flowParameters, StackView stack, Long stackId) {
        super(flowParameters);
        this.stack = stack;
        this.provisionType = ProvisionType.REGULAR;
        this.stackId = stackId;
    }

    public StackFailureContext(FlowParameters flowParameters, StackView stack, Long stackId, ProvisionType provisionType) {
        super(flowParameters);
        this.stack = stack;
        this.provisionType = provisionType;
        this.stackId = stackId;
    }

    public StackView getStack() {
        return stack;
    }

    public Long getStackId() {
        return stackId;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
