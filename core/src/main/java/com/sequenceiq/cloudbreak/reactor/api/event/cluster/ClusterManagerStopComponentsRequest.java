package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class ClusterManagerStopComponentsRequest extends AmbariComponentsRequest implements FlowPayload {
    @JsonCreator
    public ClusterManagerStopComponentsRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("hostName") String hostName,
            @JsonProperty("components") Map<String, String> components) {
        super(stackId, hostGroups, hostName, components);
    }
}
