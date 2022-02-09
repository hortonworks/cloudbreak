package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmUpgradePreparationResult extends StackEvent {
    public CcmUpgradePreparationResult(Long stackId) {
        super(stackId);
    }

    public CcmUpgradePreparationResult(String selector, Long stackId) {
        super(selector, stackId);
    }
}
