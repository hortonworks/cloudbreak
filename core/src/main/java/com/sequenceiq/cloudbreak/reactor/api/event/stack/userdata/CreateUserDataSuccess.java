package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CreateUserDataSuccess extends StackEvent {
    public CreateUserDataSuccess(Long stackId) {
        super(stackId);
    }
}
