package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MountDisksRequest extends StackEvent {
    public MountDisksRequest(Long stackId) {
        super(stackId);
    }
}
