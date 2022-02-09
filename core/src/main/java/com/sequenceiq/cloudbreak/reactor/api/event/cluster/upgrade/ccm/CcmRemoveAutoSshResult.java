package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmRemoveAutoSshResult extends StackEvent {
    public CcmRemoveAutoSshResult(Long stackId) {
        super(stackId);
    }

    public CcmRemoveAutoSshResult(String request, Long stackId) {
        super(request, stackId);
    }
}
