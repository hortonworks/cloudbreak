package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpgradeCcmRegisterClusterProxyResult extends StackEvent {

    public UpgradeCcmRegisterClusterProxyResult(Long stackId) {
        super(stackId);
    }

    public UpgradeCcmRegisterClusterProxyResult(String selector, Long stackId) {
        super(selector, stackId);
    }
}
