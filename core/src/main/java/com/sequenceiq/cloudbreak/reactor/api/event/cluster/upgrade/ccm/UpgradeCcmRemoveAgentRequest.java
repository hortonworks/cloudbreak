package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmRemoveAgentRequest extends StackEvent {

    public UpgradeCcmRemoveAgentRequest(Long stackId) {
        super(stackId);
    }
}
