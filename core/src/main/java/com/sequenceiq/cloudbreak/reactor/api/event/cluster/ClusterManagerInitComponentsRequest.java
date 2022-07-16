package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterManagerInitComponentsRequest extends AmbariComponentsRequest {

    @JsonCreator
    public ClusterManagerInitComponentsRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("hostName") String hostName,
            @JsonProperty("components") Map<String, String> components) {
        super(stackId, hostGroups, hostName, components);
    }
}
