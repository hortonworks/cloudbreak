package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmHealthCheckRequest extends StackEvent {

    public UpgradeCcmHealthCheckRequest(Long stackId) {
        super(stackId);
    }
}
