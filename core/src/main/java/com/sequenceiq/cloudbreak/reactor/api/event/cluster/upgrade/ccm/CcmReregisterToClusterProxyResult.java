package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmReregisterToClusterProxyResult extends StackEvent {
    public CcmReregisterToClusterProxyResult(Long stackId) {
        super(stackId);
    }

    public CcmReregisterToClusterProxyResult(String selector, Long stackId) {
        super(selector, stackId);
    }
}
