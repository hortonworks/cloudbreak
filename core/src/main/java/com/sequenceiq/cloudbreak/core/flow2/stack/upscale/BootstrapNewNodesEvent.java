package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapNewNodesEvent extends StackEvent {
    private Set<String> upscaleCandidateAddresses;

    public BootstrapNewNodesEvent(Long stackId, Set<String> upscaleCandidateAddresses) {
        super(stackId);
        this.upscaleCandidateAddresses = upscaleCandidateAddresses;
    }

    public Set<String> getUpscaleCandidateAddresses() {
        return upscaleCandidateAddresses;
    }
}
