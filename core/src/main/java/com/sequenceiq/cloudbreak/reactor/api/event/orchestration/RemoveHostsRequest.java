package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class RemoveHostsRequest extends AbstractClusterScaleRequest {
    private Set<String> hostNames;

    public RemoveHostsRequest(Long stackId, Set<String> hostGroups, Set<String> hostNames) {
        super(stackId, hostGroups);
        this.hostNames = hostNames;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
