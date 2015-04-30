package com.sequenceiq.cloudbreak.service.cluster.event;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.ScalingType;

public class UpdateAmbariHostsRequest {

    private Long stackId;
    private CloudPlatform cloudPlatform;
    private HostGroupAdjustmentJson hostGroupAdjustment;
    private List<HostMetadata> decommissionCandidates;
    private Set<String> upscaleCandidateAddresses;
    private boolean decommission;
    private ScalingType scalingType;

    public UpdateAmbariHostsRequest(Long stackId, HostGroupAdjustmentJson adjustmentJson, Set<String> upscaleCandidateAddresses,
            List<HostMetadata> decommissionCandidates, boolean decommission, CloudPlatform cloudPlatform, ScalingType scalingType) {
        this.stackId = stackId;
        this.decommissionCandidates = decommissionCandidates;
        this.hostGroupAdjustment = adjustmentJson;
        this.decommission = decommission;
        this.cloudPlatform = cloudPlatform;
        this.scalingType = scalingType;
        this.upscaleCandidateAddresses = upscaleCandidateAddresses;
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

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public Set<String> getUpscaleCandidateAddresses() {
        return upscaleCandidateAddresses;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
