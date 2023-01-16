package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy;

import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ModifyProxyConfigContext extends CommonContext {

    private final StackView stack;

    private final String previousProxyConfigCrn;

    public ModifyProxyConfigContext(FlowParameters flowParameters, StackView stack, String previousProxyConfigCrn) {
        super(flowParameters);
        this.stack = stack;
        this.previousProxyConfigCrn = previousProxyConfigCrn;
    }

    public StackView getStack() {
        return stack;
    }

    public String getPreviousProxyConfigCrn() {
        return previousProxyConfigCrn;
    }
}
