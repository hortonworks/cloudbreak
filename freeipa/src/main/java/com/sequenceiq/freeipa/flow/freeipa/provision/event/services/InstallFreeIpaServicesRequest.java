package com.sequenceiq.freeipa.flow.freeipa.provision.event.services;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class InstallFreeIpaServicesRequest extends StackEvent {
    public InstallFreeIpaServicesRequest(Long stackId) {
        super(stackId);
    }
}
