package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpgradeCcmEvent extends StackEvent {

    private Tunnel oldTunnel;

    @JsonCreator
    public UpgradeCcmEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel) {
        super(selector, stackId);
        this.oldTunnel = oldTunnel;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public void setOldTunnel(Tunnel oldTunnel) {
        this.oldTunnel = oldTunnel;
    }

    @Override
    public String toString() {
        return "UpgradeCcmEvent{" +
                ", oldTunnel=" + oldTunnel +
                "} " + super.toString();
    }
}
