package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.type.Tunnel;

public abstract class AbstractUpgradeCcmEvent extends StackEvent {

    private final Long clusterId;

    private final Tunnel oldTunnel;

    public AbstractUpgradeCcmEvent(Long stackId, Long clusterId, Tunnel oldTunnel) {
        super(stackId);
        this.clusterId = clusterId;
        this.oldTunnel = oldTunnel;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }
}
