package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;

public class StackFailureContext extends CommonContext {

    private final StackView stackView;

    public StackFailureContext(String flowId, StackView stackView) {
        super(flowId);
        this.stackView = stackView;
    }

    public StackView getStackView() {
        return stackView;
    }

}
