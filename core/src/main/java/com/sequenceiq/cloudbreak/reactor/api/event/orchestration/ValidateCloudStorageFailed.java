package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ValidateCloudStorageFailed extends StackFailureEvent {
    public ValidateCloudStorageFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
