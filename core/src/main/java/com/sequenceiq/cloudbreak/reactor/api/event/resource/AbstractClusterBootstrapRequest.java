package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

import java.util.Set;

public abstract class AbstractClusterBootstrapRequest extends ClusterPlatformRequest {
    private final Set<String> upscaleCandidateAddresses;

    protected AbstractClusterBootstrapRequest(Long stackId, Set<String> upscaleCandidateAddresses) {
        super(stackId);
        this.upscaleCandidateAddresses = upscaleCandidateAddresses;
    }

    public Set<String> getUpscaleCandidateAddresses() {
        return upscaleCandidateAddresses;
    }
}
