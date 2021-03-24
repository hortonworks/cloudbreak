package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;

public class CollectDownscaleCandidatesRequest extends AbstractClusterScaleRequest {
    private final Integer scalingAdjustment;

    private final ClusterDownscaleDetails details;

    private final Set<Long> privateIds;

    public CollectDownscaleCandidatesRequest(Long stackId, String hostGroupName, Integer scalingAdjustment, Set<Long> privateIds,
            ClusterDownscaleDetails details) {
        super(stackId, hostGroupName);
        this.scalingAdjustment = scalingAdjustment;
        this.privateIds = privateIds;
        this.details = details;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "CollectDownscaleCandidatesRequest{" +
                "stackId=" + getResourceId() +
                ", hostGroupName=" + getHostGroupName() +
                ", scalingAdjustment=" + scalingAdjustment +
                ", details=" + details +
                ", privateIds=" + privateIds +
                '}';
    }

}
