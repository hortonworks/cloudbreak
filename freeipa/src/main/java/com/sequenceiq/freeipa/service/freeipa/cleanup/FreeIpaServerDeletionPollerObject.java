package com.sequenceiq.freeipa.service.freeipa.cleanup;

import java.util.Set;

public class FreeIpaServerDeletionPollerObject {

    private final Long stackId;

    private final Set<String> hosts;

    public FreeIpaServerDeletionPollerObject(Long stackId, Set<String> hosts) {
        this.stackId = stackId;
        this.hosts = hosts;
    }

    public Long getStackId() {
        return stackId;
    }

    public Set<String> getHosts() {
        return hosts;
    }
}
