package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

public class UpgradeCcmFailedEvent extends StackFailureEvent {

    private final Tunnel oldTunnel;

    private final Class<? extends ExceptionCatcherEventHandler<? extends AbstractUpgradeCcmEvent>> failureOrigin;

    public UpgradeCcmFailedEvent(Long stackId, Tunnel oldTunnel,
            Class<? extends ExceptionCatcherEventHandler<? extends AbstractUpgradeCcmEvent>> failureOrigin, Exception ex) {

        super(stackId, ex);
        this.oldTunnel = oldTunnel;
        this.failureOrigin = failureOrigin;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public Class<? extends ExceptionCatcherEventHandler<? extends AbstractUpgradeCcmEvent>> getFailureOrigin() {
        return failureOrigin;
    }

    @Override
    public String toString() {
        return "UpgradeCcmFailedEvent{" +
                "oldTunnel=" + oldTunnel +
                ", failureOrigin=" + failureOrigin +
                "} " + super.toString();
    }
}
