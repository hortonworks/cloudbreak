package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class CreateUserDataFailed extends StackFailureEvent {
    public CreateUserDataFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
