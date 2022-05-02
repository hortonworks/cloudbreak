package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmReconfigureNginxRequest extends StackEvent {

    public UpgradeCcmReconfigureNginxRequest(Long stackId) {
        super(stackId);
    }
}
