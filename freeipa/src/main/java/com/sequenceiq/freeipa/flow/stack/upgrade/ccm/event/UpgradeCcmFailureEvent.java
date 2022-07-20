package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

<<<<<<< HEAD
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_CCM_FAILED;

import java.time.LocalDateTime;
=======
import java.time.LocalDateTime;
import java.util.Optional;
>>>>>>> CB-14585 ccm upgrade revert flow in case of error in freeipa

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.AbstractUpgradeCcmEventHandler;

public class UpgradeCcmFailureEvent extends StackFailureEvent {

    private final Tunnel oldTunnel;

<<<<<<< HEAD
    private final LocalDateTime revertTime;

    private final DetailedStackStatus transitionStatusAfterFailure;
=======
    private LocalDateTime revertTime;

    private final Optional<DetailedStackStatus> transitionStatusAfterFailure;
>>>>>>> CB-14585 ccm upgrade revert flow in case of error in freeipa

    private final Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin;

    private String statusReason;

    public UpgradeCcmFailureEvent(String selector, Long stackId, Tunnel oldTunnel,
            Class<? extends AbstractUpgradeCcmEventHandler> failureOrigin, Exception exception, LocalDateTime revertTime, String statusReason) {

        super(selector, stackId, exception);
        this.oldTunnel = oldTunnel;
        this.failureOrigin = failureOrigin;
<<<<<<< HEAD
        this.transitionStatusAfterFailure = null;
=======
        this.transitionStatusAfterFailure = Optional.empty();
>>>>>>> CB-14585 ccm upgrade revert flow in case of error in freeipa
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
<<<<<<< HEAD
            @JsonProperty("transitionStatusAfterFailure") DetailedStackStatus transitionStatus,
=======
            @JsonProperty("transitionStatusAfterFailure") Optional<DetailedStackStatus> transitionStatus,
>>>>>>> CB-14585 ccm upgrade revert flow in case of error in freeipa
            @JsonProperty("revertTime") LocalDateTime revertTime,
            @JsonProperty("statusReason") String statusReason) {
        super(selector, stackId, exception);
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

<<<<<<< HEAD
=======
    public void setUpgradeDateTime(LocalDateTime plusMinutes) {
        revertTime = plusMinutes;
    }

    public void setRevertTime(LocalDateTime revertTime) {
        this.revertTime = revertTime;
    }

>>>>>>> CB-14585 ccm upgrade revert flow in case of error in freeipa
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public LocalDateTime getRevertTime() {
        return revertTime;
    }

<<<<<<< HEAD
=======
    public UpgradeCcmFailureEvent withStatusReason(String statusReason) {
        this.statusReason = statusReason;
        return this;
    }

>>>>>>> CB-14585 ccm upgrade revert flow in case of error in freeipa
    @Override
    public String toString() {
        return "UpgradeCcmFailureEvent{" +
                "oldTunnel=" + oldTunnel +
                ", transitionStatusAfterFailure=" + transitionStatusAfterFailure +
                ", failureOrigin=" + failureOrigin +
                ", statusReason=" + statusReason +
<<<<<<< HEAD
                ", revertTime=" + revertTime +
=======
>>>>>>> CB-14585 ccm upgrade revert flow in case of error in freeipa
                "} " + super.toString();
    }

    public String getStatusReason() {
        return statusReason;
    }
}
