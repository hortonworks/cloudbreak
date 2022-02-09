package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmReregisterToClusterProxyRequest extends StackEvent {
    public CcmReregisterToClusterProxyRequest(Long stackId) {
        super(stackId);
    }
}
