package com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ValidateCloudStorageFailed extends StackFailureEvent {
    public ValidateCloudStorageFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
