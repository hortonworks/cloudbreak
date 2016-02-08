package com.sequenceiq.cloudbreak.service.cluster.event;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.domain.HostMetadata;

public class UpdateAmbariHostsRequest {

    private Long stackId;
    private Platform cloudPlatform;
    private HostGroupAdjustmentJson hostGroupAdjustment;
    private List<HostMetadata> decommissionCandidates;
    private boolean decommission;
    private ScalingType scalingType;

    public UpdateAmbariHostsRequest(Long stackId, HostGroupAdjustmentJson adjustmentJson,
            List<HostMetadata> decommissionCandidates, boolean decommission, Platform cloudPlatform, ScalingType scalingType) {
        this.stackId = stackId;
        this.decommissionCandidates = decommissionCandidates;
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

    public List<HostMetadata> getDecommissionCandidates() {
        return decommissionCandidates;
    }

    public Platform getCloudPlatform() {
        return cloudPlatform;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
