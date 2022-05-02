package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmTunnelUpdateRequest extends StackEvent {
    public UpgradeCcmTunnelUpdateRequest(Long stackId) {
        super(stackId);
    }
}
