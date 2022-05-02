package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmPushSaltStatesRequest extends StackEvent {

    public UpgradeCcmPushSaltStatesRequest(Long stackId) {
        super(stackId);
    }
}
