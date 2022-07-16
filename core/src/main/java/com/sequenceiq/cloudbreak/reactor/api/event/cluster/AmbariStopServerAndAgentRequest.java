package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariStopServerAndAgentRequest extends AbstractClusterScaleRequest {
    @JsonCreator
    public AmbariStopServerAndAgentRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups) {
        super(stackId, hostGroups);
    }
}
