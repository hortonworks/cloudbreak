package com.sequenceiq.cloudbreak.service.cluster.event;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ScalingType;

public class UpdateAmbariHostsRequest {

    private Long stackId;
    private Platform cloudPlatform;
    private HostGroupAdjustmentJson hostGroupAdjustment;
    private boolean decommission;
    private ScalingType scalingType;

    public UpdateAmbariHostsRequest(Long stackId, HostGroupAdjustmentJson adjustmentJson, boolean decommission, Platform cloudPlatform,
            ScalingType scalingType) {
        this.stackId = stackId;
        this.hostGroupAdjustment = adjustmentJson;
        this.decommission = decommission;
        this.cloudPlatform = cloudPlatform;
        this.scalingType = scalingType;
    }

    public Long getStackId() {
        return stackId;
    }

    public HostGroupAdjustmentJson getHostGroupAdjustment() {
        return hostGroupAdjustment;
    }

    public boolean isDecommission() {
        return decommission;
    }

    public Platform getCloudPlatform() {
        return cloudPlatform;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
