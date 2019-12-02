package com.sequenceiq.freeipa.flow.stack.provision.event.userdata;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class CreateUserDataFailed extends StackFailureEvent {
    public CreateUserDataFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
