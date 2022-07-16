package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariGatherInstalledComponentsRequest extends AbstractClusterScaleRequest {

    private final String hostName;

    @JsonCreator
    public AmbariGatherInstalledComponentsRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroupNames,
            @JsonProperty("hostName") String hostName) {
        super(stackId, hostGroupNames);
        this.hostName = hostName;
    }

    public String getHostName() {
        return hostName;
    }
}
