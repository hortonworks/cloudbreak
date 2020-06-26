package com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PostInstallFreeIpaRequest extends StackEvent {

    private final boolean fullPostInstall;

    public PostInstallFreeIpaRequest(Long stackId) {
        this(stackId, true);
    }

    public PostInstallFreeIpaRequest(Long stackId, boolean fullPostInstall) {
        super(stackId);
        this.fullPostInstall = fullPostInstall;
    }

    public boolean isFullPostInstall() {
        return fullPostInstall;
    }
}
