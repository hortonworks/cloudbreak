package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class CcmUpgradePreparationFailed extends StackFailureEvent {
    public CcmUpgradePreparationFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
