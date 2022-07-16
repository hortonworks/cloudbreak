package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BootstrapNewNodesRequest extends AbstractClusterBootstrapRequest {

    private final Set<String> hostNames;

    @JsonCreator
    public BootstrapNewNodesRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("upscaleCandidateAddresses") Set<String> upscaleCandidateAddresses,
            @JsonProperty("hostNames") Set<String> hostNames) {
        super(stackId, upscaleCandidateAddresses);
        this.hostNames = hostNames;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
