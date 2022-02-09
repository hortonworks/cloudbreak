package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmRemoveAutoSshRequest extends StackEvent {
    public CcmRemoveAutoSshRequest(Long stackId) {
        super(stackId);
    }
}
