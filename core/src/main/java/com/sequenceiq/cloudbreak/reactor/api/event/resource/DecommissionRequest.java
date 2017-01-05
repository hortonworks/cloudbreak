package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class DecommissionRequest extends AbstractClusterScaleRequest {

    private final Set<String> hostNames;

    public DecommissionRequest(Long stackId, String hostGroupName, Set<String> hostNames) {
        super(stackId, hostGroupName);
        this.hostNames = hostNames;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
