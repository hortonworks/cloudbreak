package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class StackFailureContext extends CommonContext {

    private final StackView stackView;

    private final ProvisionType provisionType;

    public StackFailureContext(FlowParameters flowParameters, StackView stackView) {
        super(flowParameters);
        this.stackView = stackView;
        this.provisionType = ProvisionType.REGULAR;
    }

    public StackFailureContext(FlowParameters flowParameters, StackView stackView, ProvisionType provisionType) {
        super(flowParameters);
        this.stackView = stackView;
        this.provisionType = provisionType;
    }

    public StackView getStackView() {
        return stackView;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
