package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmUnregisterHostsResult extends StackEvent {
    public CcmUnregisterHostsResult(Long stackId) {
        super(stackId);
    }

    public CcmUnregisterHostsResult(String selector, Long stackId) {
        super(selector, stackId);
    }
}
