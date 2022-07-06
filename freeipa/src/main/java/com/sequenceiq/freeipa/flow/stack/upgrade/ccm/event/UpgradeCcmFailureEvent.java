package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonCreator
    public UpgradeCcmFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonProperty("failureOrigin") Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("transitionStatusAfterFailure") Optional<DetailedStackStatus> transitionStatus) {

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
