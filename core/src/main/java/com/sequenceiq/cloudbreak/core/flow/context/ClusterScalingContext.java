package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;

public class ClusterScalingContext extends DefaultFlowContext {

    private HostGroupAdjustmentJson hostGroupAdjustment;
    private List<HostMetadata> candidates;
    private ScalingType scalingType;

    public ClusterScalingContext(Long stackId, Platform cloudPlatform, HostGroupAdjustmentJson hostGroupAdjustment,
            List<HostMetadata> candidates, ScalingType scalingType) {
        super(stackId, cloudPlatform);
        this.hostGroupAdjustment = hostGroupAdjustment;
        this.candidates = candidates;
        this.scalingType = scalingType;
    }

    public ClusterScalingContext(UpdateAmbariHostsRequest updateAmbariHostsRequest) {
        super(updateAmbariHostsRequest.getStackId(), updateAmbariHostsRequest.getCloudPlatform());
        this.hostGroupAdjustment = updateAmbariHostsRequest.getHostGroupAdjustment();
        this.candidates = updateAmbariHostsRequest.getDecommissionCandidates();
        this.scalingType = updateAmbariHostsRequest.getScalingType();
    }

    public HostGroupAdjustmentJson getHostGroupAdjustment() {
        return hostGroupAdjustment;
    }

    public List<HostMetadata> getCandidates() {
        return candidates;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

}
