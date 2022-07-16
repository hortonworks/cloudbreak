package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;

public class CollectDownscaleCandidatesRequest extends AbstractClusterScaleRequest {

    private final ClusterDownscaleDetails details;

    private final Map<String, Integer> hostGroupWithAdjustment;

    private final Map<String, Set<Long>> hostGroupWithPrivateIds;

    @JsonCreator
    public CollectDownscaleCandidatesRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupWithAdjustment") Map<String, Integer> hostGroupWithAdjustment,
            @JsonProperty("hostGroupWithPrivateIds") Map<String, Set<Long>> hostGroupWithPrivateIds,
            @JsonProperty("details") ClusterDownscaleDetails details) {
        super(stackId, !hostGroupWithAdjustment.isEmpty() ? hostGroupWithAdjustment.keySet() : hostGroupWithPrivateIds.keySet());
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.hostGroupWithPrivateIds = hostGroupWithPrivateIds;
        this.details = details;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }

    public Map<String, Set<Long>> getHostGroupWithPrivateIds() {
        return hostGroupWithPrivateIds;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "CollectDownscaleCandidatesRequest{" +
                "stackId=" + getResourceId() +
                ", hostGroupWithAdjustment=" + hostGroupWithAdjustment +
                ", hostGroupWithPrivateIds=" + hostGroupWithPrivateIds +
                ", details=" + details +
                '}';
    }

}
