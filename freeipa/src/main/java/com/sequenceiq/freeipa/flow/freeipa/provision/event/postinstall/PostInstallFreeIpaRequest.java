package com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PostInstallFreeIpaRequest extends StackEvent {
    public PostInstallFreeIpaRequest(Long stackId) {
        super(stackId);
    }
}
