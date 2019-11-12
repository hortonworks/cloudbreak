package com.sequenceiq.freeipa.flow.stack.provision.event.userdata;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CreateUserDataSuccess extends StackEvent {
    public CreateUserDataSuccess(Long stackId) {
        super(stackId);
    }
}
