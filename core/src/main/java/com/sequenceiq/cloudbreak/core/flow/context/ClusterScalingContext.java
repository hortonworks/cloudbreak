package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;

public class ClusterScalingContext extends DefaultFlowContext {

    private HostGroupAdjustmentJson hostGroupAdjustment;
    private ScalingType scalingType;

    public ClusterScalingContext(Long stackId, Platform cloudPlatform, HostGroupAdjustmentJson hostGroupAdjustment, ScalingType scalingType) {
        super(stackId, cloudPlatform);
        this.hostGroupAdjustment = hostGroupAdjustment;
        this.scalingType = scalingType;
    }

    public ClusterScalingContext(UpdateAmbariHostsRequest updateAmbariHostsRequest) {
        super(updateAmbariHostsRequest.getStackId(), updateAmbariHostsRequest.getCloudPlatform());
        this.hostGroupAdjustment = updateAmbariHostsRequest.getHostGroupAdjustment();
        this.scalingType = updateAmbariHostsRequest.getScalingType();
    }

    public HostGroupAdjustmentJson getHostGroupAdjustment() {
        return hostGroupAdjustment;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

}
