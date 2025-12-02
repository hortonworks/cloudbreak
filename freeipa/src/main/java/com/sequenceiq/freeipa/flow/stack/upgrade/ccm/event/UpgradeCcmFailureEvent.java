package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_CCM_FAILED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.AbstractUpgradeCcmEventHandler;

public class UpgradeCcmFailureEvent extends StackFailureEvent {

    private final Tunnel oldTunnel;

    private final LocalDateTime revertTime;

    private final DetailedStackStatus transitionStatusAfterFailure;

    private final Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin;

    private String statusReason;

    public UpgradeCcmFailureEvent(
            String selector,
            Long stackId,
            Tunnel oldTunnel,
            Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin,
            Exception exception,
            LocalDateTime revertTime,
            String statusReason,
            FailureType failureType) {
        super(selector, stackId, exception, failureType);
        this.oldTunnel = oldTunnel;
        this.failureOrigin = failureOrigin;
        this.transitionStatusAfterFailure = null;
        this.revertTime = revertTime;
        this.statusReason = statusReason;
    }

    @JsonCreator
    public UpgradeCcmFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonProperty("failureOrigin") Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("transitionStatusAfterFailure") DetailedStackStatus transitionStatus,
            @JsonProperty("revertTime") LocalDateTime revertTime,
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("failureType") FailureType failureType) {
        super(selector, stackId, exception, failureType);
        this.oldTunnel = oldTunnel;
        this.failureOrigin = failureOrigin;
        this.transitionStatusAfterFailure = transitionStatus;
        this.revertTime = revertTime;
        this.statusReason = statusReason;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public DetailedStackStatus getTransitionStatusAfterFailure() {
        return transitionStatusAfterFailure == null ? UPGRADE_CCM_FAILED : transitionStatusAfterFailure;
    }

    public Class<? extends AbstractUpgradeCcmEventHandler> getFailureOrigin() {
        return failureOrigin;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public LocalDateTime getRevertTime() {
        return revertTime;
    }

    @Override
    public String toString() {
        return "UpgradeCcmFailureEvent{" +
                "oldTunnel=" + oldTunnel +
                ", transitionStatusAfterFailure=" + transitionStatusAfterFailure +
                ", failureOrigin=" + failureOrigin +
                ", statusReason=" + statusReason +
                ", revertTime=" + revertTime +
                "} " + super.toString();
    }

    public String getStatusReason() {
        return statusReason;
    }
}
