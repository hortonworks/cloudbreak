package com.sequenceiq.cloudbreak.reactor.api.event.kerberos;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class KeytabConfigurationFailed extends StackFailureEvent {
    public KeytabConfigurationFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
