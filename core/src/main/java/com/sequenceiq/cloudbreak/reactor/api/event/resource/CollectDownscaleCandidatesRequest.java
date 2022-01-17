package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;

public class CollectDownscaleCandidatesRequest extends AbstractClusterScaleRequest {

    private final ClusterDownscaleDetails details;

    private final Map<String, Integer> hostGroupWithAdjustment;

    private final Map<String, Set<Long>> hostGroupWithPrivateIds;

    public CollectDownscaleCandidatesRequest(Long stackId, Map<String, Integer> hostGroupWithAdjustment, Map<String, Set<Long>> hostGroupWithPrivateIds,
            ClusterDownscaleDetails details) {
        super(stackId, hostGroupWithAdjustment.size() > 0 ? hostGroupWithAdjustment.keySet() : hostGroupWithPrivateIds.keySet());
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
