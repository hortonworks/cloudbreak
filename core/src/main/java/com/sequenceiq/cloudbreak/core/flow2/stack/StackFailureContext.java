package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.StackView;

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
