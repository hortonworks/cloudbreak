package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ValidateCloudStorageRequest extends StackEvent {
    public ValidateCloudStorageRequest(Long stackId) {
        super(stackId);
    }
}
