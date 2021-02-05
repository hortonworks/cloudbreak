package com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ValidateCloudStorageSuccess extends StackEvent {
    public ValidateCloudStorageSuccess(Long stackId) {
        super(stackId);
    }
}
