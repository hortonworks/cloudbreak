package com.sequenceiq.cloudbreak.reactor.api.event.kerberos;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class KeytabConfigurationRequest extends StackEvent {
    public KeytabConfigurationRequest(Long stackId) {
        super(stackId);
    }
}
