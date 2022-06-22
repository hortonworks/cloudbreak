package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Optional;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.AbstractUpgradeCcmEventHandler;

public class UpgradeCcmFailureEvent extends StackFailureEvent {

    private final Tunnel oldTunnel;

    private final Optional<DetailedStackStatus> transitionStatusAfterFailure;

    private final Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin;

    public UpgradeCcmFailureEvent(String selector, Long stackId, Tunnel oldTunnel,
            Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin, Exception exception) {

        super(selector, stackId, exception);
        this.oldTunnel = oldTunnel;
        this.failureOrigin = failureOrigin;
        this.transitionStatusAfterFailure = Optional.empty();
    }

    public UpgradeCcmFailureEvent(String selector, Long stackId, Tunnel oldTunnel,
            Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin, Exception exception, Optional<DetailedStackStatus> transitionStatus) {

        super(selector, stackId, exception);
        this.oldTunnel = oldTunnel;
        this.failureOrigin = failureOrigin;
        this.transitionStatusAfterFailure = transitionStatus;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public Optional<DetailedStackStatus> getTransitionStatusAfterFailure() {
        return transitionStatusAfterFailure;
    }

    public Class<? extends AbstractUpgradeCcmEventHandler> getFailureOrigin() {
        return failureOrigin;
    }

    @Override
    public String toString() {
        return "UpgradeCcmFailureEvent{" +
                "oldTunnel=" + oldTunnel +
                ", transitionStatusAfterFailure=" + transitionStatusAfterFailure +
                ", failureOrigin=" + failureOrigin +
                "} " + super.toString();
    }
}
