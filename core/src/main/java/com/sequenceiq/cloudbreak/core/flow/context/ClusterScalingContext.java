package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.ScalingType;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;

public class ClusterScalingContext extends DefaultFlowContext implements FlowContext {

    private HostGroupAdjustmentJson hostGroupAdjustment;
    private List<HostMetadata> candidates;
    private Set<String> upscaleCandidateAddresses;
    private ScalingType scalingType;

    public ClusterScalingContext(Long stackId, CloudPlatform cloudPlatform, HostGroupAdjustmentJson hostGroupAdjustment, Set<String> upscaleCandidateAddresses,
            List<HostMetadata> candidates, ScalingType scalingType) {
        super(stackId, cloudPlatform);
        this.hostGroupAdjustment = hostGroupAdjustment;
        this.candidates = candidates;
        this.scalingType = scalingType;
        this.upscaleCandidateAddresses = upscaleCandidateAddresses;
    }

    public ClusterScalingContext(UpdateAmbariHostsRequest updateAmbariHostsRequest) {
        super(updateAmbariHostsRequest.getStackId(), updateAmbariHostsRequest.getCloudPlatform());
        this.hostGroupAdjustment = updateAmbariHostsRequest.getHostGroupAdjustment();
        this.candidates = updateAmbariHostsRequest.getDecommissionCandidates();
        this.scalingType = updateAmbariHostsRequest.getScalingType();
        this.upscaleCandidateAddresses = updateAmbariHostsRequest.getUpscaleCandidateAddresses();
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

    public Set<String> getUpscaleCandidateAddresses() {
        return upscaleCandidateAddresses;
    }
}
