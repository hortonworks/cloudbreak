package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;

public class DecommissionRequest extends AbstractClusterScaleRequest {

    private final Set<Long> privateIds;

    private final ClusterDownscaleDetails details;

    @JsonCreator
    public DecommissionRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("privateIds") Set<Long> privateIds,
            @JsonProperty("details") ClusterDownscaleDetails details) {
        super(stackId, hostGroups);
        this.privateIds = privateIds;
        this.details = details;
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "DecommissionRequest{" +
                "privateIds=" + privateIds +
                ", details=" + details +
                "} " + super.toString();
    }
}
