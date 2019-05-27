package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class StackFailureContext extends CommonContext {

    private final StackView stackView;

    public StackFailureContext(FlowParameters flowParameters, StackView stackView) {
        super(flowParameters);
        this.stackView = stackView;
    }

    public StackView getStackView() {
        return stackView;
    }

}
