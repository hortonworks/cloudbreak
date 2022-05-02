package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmRemoveAgentResult extends StackEvent {
    public UpgradeCcmRemoveAgentResult(Long stackId) {
        super(stackId);
    }

    public UpgradeCcmRemoveAgentResult(String request, Long stackId) {
        super(request, stackId);
    }
}
