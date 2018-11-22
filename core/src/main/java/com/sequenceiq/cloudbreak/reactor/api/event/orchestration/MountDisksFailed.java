package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class MountDisksFailed extends StackFailureEvent {
    public MountDisksFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
