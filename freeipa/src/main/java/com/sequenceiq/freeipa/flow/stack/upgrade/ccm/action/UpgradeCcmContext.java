package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;

public class UpgradeCcmContext extends CommonContext {

    private final Stack stack;

    public UpgradeCcmContext(FlowParameters flowParameters, Stack stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }

}
