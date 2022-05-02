package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmHealthCheckResult extends StackEvent {
    public UpgradeCcmHealthCheckResult(Long stackId) {
        super(stackId);
    }

    public UpgradeCcmHealthCheckResult(String request, Long stackId) {
        super(request, stackId);
    }
}
