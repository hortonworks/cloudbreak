package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmTunnelUpdateResult extends StackEvent {

    public UpgradeCcmTunnelUpdateResult(Long stackId) {
        super(stackId);
    }

    public UpgradeCcmTunnelUpdateResult(String selector, Long stackId) {
        super(selector, stackId);
    }
}
