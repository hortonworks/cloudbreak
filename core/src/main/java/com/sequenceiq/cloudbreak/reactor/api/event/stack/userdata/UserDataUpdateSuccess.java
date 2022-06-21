package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UserDataUpdateSuccess extends StackEvent {
    public UserDataUpdateSuccess(Long stackId) {
        super(stackId);
    }
}
