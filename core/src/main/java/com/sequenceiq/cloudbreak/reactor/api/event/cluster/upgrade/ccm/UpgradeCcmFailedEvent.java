package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpgradeCcmFailedEvent extends StackFailureEvent {
    public UpgradeCcmFailedEvent(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
