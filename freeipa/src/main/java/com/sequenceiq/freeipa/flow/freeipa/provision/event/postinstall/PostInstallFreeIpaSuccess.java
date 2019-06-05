package com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PostInstallFreeIpaSuccess extends StackEvent {
    public PostInstallFreeIpaSuccess(Long stackId) {
        super(stackId);
    }
}
