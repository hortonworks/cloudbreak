package com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class PostInstallFreeIpaFailed extends StackFailureEvent {
    public PostInstallFreeIpaFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
