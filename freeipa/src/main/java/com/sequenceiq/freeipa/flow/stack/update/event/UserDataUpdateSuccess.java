package com.sequenceiq.freeipa.flow.stack.update.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UserDataUpdateSuccess extends StackEvent {
    public UserDataUpdateSuccess(Long stackId) {
        super(stackId);
    }
}
