package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MountDisksSuccess extends StackEvent {
    public MountDisksSuccess(Long stackId) {
        super(stackId);
    }
}
