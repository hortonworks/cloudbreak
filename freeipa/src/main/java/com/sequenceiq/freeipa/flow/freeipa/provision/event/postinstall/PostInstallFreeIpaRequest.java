package com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PostInstallFreeIpaRequest extends StackEvent {

    private final boolean fullPostInstall;

    public PostInstallFreeIpaRequest(Long stackId) {
        this(stackId, true);
    }

    @JsonCreator
    public PostInstallFreeIpaRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("fullPostInstall") boolean fullPostInstall) {
        super(stackId);
        this.fullPostInstall = fullPostInstall;
    }

    public boolean isFullPostInstall() {
        return fullPostInstall;
    }
}
