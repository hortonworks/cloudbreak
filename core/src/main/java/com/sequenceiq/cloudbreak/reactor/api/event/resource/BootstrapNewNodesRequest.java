package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class BootstrapNewNodesRequest extends AbstractClusterBootstrapRequest {

    public BootstrapNewNodesRequest(Long stackId, Set<String> upscaleCandidateAddresses) {
        super(stackId, upscaleCandidateAddresses);
    }

}
