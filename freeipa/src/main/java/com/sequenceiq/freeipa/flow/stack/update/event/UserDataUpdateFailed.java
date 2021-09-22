package com.sequenceiq.freeipa.flow.stack.update.event;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class UserDataUpdateFailed extends StackFailureEvent {
    public UserDataUpdateFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    public UserDataUpdateFailed(String selector, Long stackId, Exception exception) {
        super(selector, stackId, exception);
    }
}
