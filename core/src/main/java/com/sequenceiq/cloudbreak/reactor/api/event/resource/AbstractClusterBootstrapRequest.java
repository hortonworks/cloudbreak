package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public abstract class AbstractClusterBootstrapRequest extends ClusterPlatformRequest {
    private final Set<String> upscaleCandidateAddresses;

    public AbstractClusterBootstrapRequest(Long stackId, Set<String> upscaleCandidateAddresses) {
        super(stackId);
        this.upscaleCandidateAddresses = upscaleCandidateAddresses;
    }

    public Set<String> getUpscaleCandidateAddresses() {
        return upscaleCandidateAddresses;
    }
}
