package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import java.time.LocalDateTime;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.type.Tunnel;

import reactor.rx.Promise;

public abstract class AbstractUpgradeCcmEvent extends StackEvent implements UpgradeCcmBaseEvent {

    private final Long clusterId;

    private final Tunnel oldTunnel;

    private final LocalDateTime revertTime;

    public AbstractUpgradeCcmEvent(Long stackId, Long clusterId, Tunnel oldTunnel, LocalDateTime revertTime) {
        super(stackId);
        this.clusterId = clusterId;
        this.oldTunnel = oldTunnel;
        this.revertTime = revertTime;
    }

    public AbstractUpgradeCcmEvent(String selector, Long stackId, Long clusterId, Tunnel oldTunnel, LocalDateTime revertTime, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.clusterId = clusterId;
        this.oldTunnel = oldTunnel;
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
}
