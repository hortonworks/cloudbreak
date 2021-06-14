package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeInitRequest extends StackEvent {
    private final boolean patchUpgrade;

    public ClusterUpgradeInitRequest(Long stackId, boolean patchUpgrade) {
        super(stackId);
        this.patchUpgrade = patchUpgrade;
    }

    public boolean isPatchUpgrade() {
        return patchUpgrade;
    }
}
