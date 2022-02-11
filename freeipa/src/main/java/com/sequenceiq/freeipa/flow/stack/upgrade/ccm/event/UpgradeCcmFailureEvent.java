package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class UpgradeCcmFailureEvent extends StackFailureEvent {

    public UpgradeCcmFailureEvent(String selector, Long stackId, Exception exception) {
        super(selector, stackId, exception);
    }

    @Override
    public String toString() {
        return "UpgradeCcmFailureEvent{} " + super.toString();
    }

}
