package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MountDisksOnNewHostsRequest extends AbstractClusterBootstrapRequest {

    @JsonCreator
    public MountDisksOnNewHostsRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("upscaleCandidateAddresses") Set<String> upscaleCandidateAddresses) {
        super(stackId, upscaleCandidateAddresses);
    }

}
