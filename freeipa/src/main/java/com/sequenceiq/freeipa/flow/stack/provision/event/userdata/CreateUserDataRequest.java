package com.sequenceiq.freeipa.flow.stack.provision.event.userdata;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CreateUserDataRequest extends StackEvent {
    public CreateUserDataRequest(Long stackId) {
        super(stackId);
    }
}
