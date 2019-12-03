package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmKeyDeregisterSuccess extends StackEvent {

    public CcmKeyDeregisterSuccess(Long stackId) {
        super(stackId);
    }
}
