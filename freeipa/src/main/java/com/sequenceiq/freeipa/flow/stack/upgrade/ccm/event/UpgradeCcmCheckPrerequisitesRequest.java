package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpgradeCcmCheckPrerequisitesRequest extends StackEvent {

    private final Stack stack;

    public UpgradeCcmCheckPrerequisitesRequest(Stack stack) {
        super(stack.getId());
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }

}
