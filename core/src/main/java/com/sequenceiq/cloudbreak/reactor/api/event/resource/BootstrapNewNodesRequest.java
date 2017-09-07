package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class BootstrapNewNodesRequest extends AbstractClusterBootstrapRequest {

    private final Set<String> hostNames;

    public BootstrapNewNodesRequest(Long stackId, Set<String> upscaleCandidateAddresses, Set<String> hostNames) {
        super(stackId, upscaleCandidateAddresses);
        this.hostNames = hostNames;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
