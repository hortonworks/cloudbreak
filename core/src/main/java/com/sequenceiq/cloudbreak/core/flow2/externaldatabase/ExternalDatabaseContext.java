package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ExternalDatabaseContext extends CommonContext {

    private final StackView stack;

    public ExternalDatabaseContext(FlowParameters flowParameters, StackView stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public StackView getStack() {
        return stack;
    }

    public Long getStackId() {
        return stack.getId();
    }
}
