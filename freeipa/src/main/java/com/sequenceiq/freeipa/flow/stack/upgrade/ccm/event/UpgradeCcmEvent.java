package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpgradeCcmEvent extends StackEvent {

    private Tunnel oldTunnel;

    private LocalDateTime revertTime;

    private Boolean minaRemoved;

    @JsonCreator
    public UpgradeCcmEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonProperty("revertTime") LocalDateTime revertTime,
            @JsonProperty("minaRemoved") Boolean minaRemoved) {
        super(selector, stackId);
        this.oldTunnel = oldTunnel;
        this.revertTime = revertTime;
        this.minaRemoved = minaRemoved;
    }

    public UpgradeCcmEvent(
            String selector,
            Long stackId,
            Tunnel oldTunnel,
            LocalDateTime revertTime) {
        this(selector, stackId, oldTunnel, revertTime, Boolean.TRUE);
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public void setOldTunnel(Tunnel oldTunnel) {
        this.oldTunnel = oldTunnel;
    }

    public void setUpgradeDateTime(LocalDateTime plusMinutes) {
        revertTime = plusMinutes;
    }

    public LocalDateTime getRevertTime() {
        return revertTime;
    }

    public void setRevertTime(LocalDateTime revertTime) {
        this.revertTime = revertTime;
    }

    public Boolean getMinaRemoved() {
        return minaRemoved;
    }

    public void setMinaRemoved(Boolean minaRemoved) {
        this.minaRemoved = minaRemoved;
    }

    @Override
    public String toString() {
        return "UpgradeCcmEvent{" +
                " oldTunnel=" + oldTunnel +
                ", revertTime=" + revertTime +
                ", minaRemoved=" + minaRemoved +
                "} " + super.toString();
    }
}
