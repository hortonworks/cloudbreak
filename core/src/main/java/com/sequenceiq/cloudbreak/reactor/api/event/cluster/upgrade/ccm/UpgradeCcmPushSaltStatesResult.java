package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmPushSaltStatesResult extends StackEvent {
    public UpgradeCcmPushSaltStatesResult(Long stackId) {
        super(stackId);
    }

    public UpgradeCcmPushSaltStatesResult(String selector, Long stackId) {
        super(selector, stackId);
    }
}
