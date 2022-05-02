package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmRegisterClusterProxyRequest extends StackEvent {

    public UpgradeCcmRegisterClusterProxyRequest(Long stackId) {
        super(stackId);
    }
}
