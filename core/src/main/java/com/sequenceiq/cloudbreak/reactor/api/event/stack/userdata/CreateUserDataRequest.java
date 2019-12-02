package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CreateUserDataRequest extends StackEvent {
    public CreateUserDataRequest(Long stackId) {
        super(stackId);
    }
}
