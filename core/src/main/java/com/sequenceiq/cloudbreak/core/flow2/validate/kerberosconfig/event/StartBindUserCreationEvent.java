package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartBindUserCreationEvent extends StackEvent {
    public StartBindUserCreationEvent(Long stackId) {
        super(stackId);
    }
}
