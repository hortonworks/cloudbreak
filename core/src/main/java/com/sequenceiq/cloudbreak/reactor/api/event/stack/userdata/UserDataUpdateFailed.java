package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UserDataUpdateFailed extends StackFailureEvent {
    public UserDataUpdateFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    public UserDataUpdateFailed(String selector, Long stackId, Exception exception) {
        super(selector, stackId, exception);
    }
}
