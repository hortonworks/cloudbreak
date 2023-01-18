package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

public class UpgradeCcmFailedEvent extends StackFailureEvent implements UpgradeCcmBaseEvent {

    private final Tunnel oldTunnel;

    private final Long clusterId;

    private final LocalDateTime revertTime;

    private final Class<? extends ExceptionCatcherEventHandler<? extends UpgradeCcmBaseEvent>> failureOrigin;

    @JsonCreator
    public UpgradeCcmFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("clusterId") Long clusterId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonProperty("failureOrigin") Class<? extends ExceptionCatcherEventHandler<? extends UpgradeCcmBaseEvent>> failureOrigin,
            @JsonProperty("exception") Exception ex,
            @JsonProperty("revertTime") LocalDateTime revertTime) {
        super(selector, stackId, ex);
        this.oldTunnel = oldTunnel;
        this.failureOrigin = failureOrigin;
        this.clusterId = clusterId;
        this.revertTime = revertTime;
    }

    public UpgradeCcmFailedEvent(
            Long stackId,
            Long clusterId,
            Tunnel oldTunnel,
            Class<? extends ExceptionCatcherEventHandler<? extends UpgradeCcmBaseEvent>> failureOrigin,
            Exception ex,
            LocalDateTime revertTime) {

        super(stackId, ex);
        this.oldTunnel = oldTunnel;
        this.failureOrigin = failureOrigin;
        this.clusterId = clusterId;
        this.revertTime = revertTime;
    }

    @Override
    public Long getClusterId() {
        return clusterId;
    }

    @Override
    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    @Override
    public LocalDateTime getRevertTime() {
        return revertTime;
    }

    public Class<? extends ExceptionCatcherEventHandler<? extends UpgradeCcmBaseEvent>> getFailureOrigin() {
        return failureOrigin;
    }

    @Override
    public String toString() {
        return "UpgradeCcmFailedEvent{" +
                "oldTunnel=" + oldTunnel +
                ", failureOrigin=" + failureOrigin +
                ", clusterId=" + clusterId +
                ", revertTime=" + revertTime +
                "} " + super.toString();
    }
}
