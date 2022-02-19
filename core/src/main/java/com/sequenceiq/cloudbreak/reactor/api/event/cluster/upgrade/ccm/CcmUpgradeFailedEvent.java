package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class CcmUpgradeFailedEvent extends StackFailureEvent {
    public CcmUpgradeFailedEvent(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
