package com.sequenceiq.cloudbreak.reactor.api.event.kerberos;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class KeytabConfigurationSuccess extends StackEvent {
    public KeytabConfigurationSuccess(Long stackId) {
        super(stackId);
    }

    public KeytabConfigurationSuccess(String selector, Long stackId) {
        super(selector, stackId);
    }
}
