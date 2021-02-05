package com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ValidateCloudStorageRequest extends StackEvent {
    public ValidateCloudStorageRequest(Long stackId) {
        super(stackId);
    }
}
