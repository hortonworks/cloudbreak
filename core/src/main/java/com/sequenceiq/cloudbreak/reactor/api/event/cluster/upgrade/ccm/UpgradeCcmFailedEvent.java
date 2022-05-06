package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.common.api.type.Tunnel;

public class UpgradeCcmFailedEvent extends StackFailureEvent {

    private final Tunnel oldTunnel;

    public UpgradeCcmFailedEvent(Long stackId, Tunnel oldTunnel, Exception ex) {
        super(stackId, ex);
        this.oldTunnel = oldTunnel;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }
}
