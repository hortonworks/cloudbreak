package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class RemoveHostsRequest extends AbstractClusterScaleRequest {
    private final Set<String> hostNames;

    @JsonCreator
    public RemoveHostsRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("hostNames") Set<String> hostNames) {

        super(stackId, hostGroups);
        this.hostNames = hostNames;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
