package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ValidateCloudStorageSuccess extends StackEvent {
    public ValidateCloudStorageSuccess(Long stackId) {
        super(stackId);
    }
}
