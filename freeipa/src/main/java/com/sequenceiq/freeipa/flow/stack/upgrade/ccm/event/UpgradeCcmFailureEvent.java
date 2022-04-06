package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Optional;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class UpgradeCcmFailureEvent extends StackFailureEvent {

    private final Optional<DetailedStackStatus> transitionStatusAfterFailure;

    public UpgradeCcmFailureEvent(String selector, Long stackId, Exception exception) {
        super(selector, stackId, exception);
        this.transitionStatusAfterFailure = Optional.empty();
    }

    public UpgradeCcmFailureEvent(String selector, Long stackId, Exception exception, Optional<DetailedStackStatus> transitionStatus) {
        super(selector, stackId, exception);
        this.transitionStatusAfterFailure = transitionStatus;
    }

    public Optional<DetailedStackStatus> getTransitionStatusAfterFailure() {
        return transitionStatusAfterFailure;
    }

    @Override
    public String toString() {
        return "UpgradeCcmFailureEvent{" +
                "transitionStatusAfterFailure=" + transitionStatusAfterFailure +
                "} " + super.toString();
    }
}
