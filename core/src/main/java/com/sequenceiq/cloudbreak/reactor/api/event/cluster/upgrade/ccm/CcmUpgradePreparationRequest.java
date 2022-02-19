package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmUpgradePreparationRequest extends StackEvent {
    public CcmUpgradePreparationRequest(Long stackId) {
        super(stackId);
    }
}
