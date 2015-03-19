package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.List;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;

public class ClusterScalingContext implements FlowContext {

    private Long stackId;
    private HostGroupAdjustmentJson hostGroupAdjustment;
    private List<HostMetadata> decommissionCandidates;
    private String errorReason = "";

    public ClusterScalingContext() { }

    public ClusterScalingContext(UpdateAmbariHostsRequest updateAmbariHostsRequest) {
        this.stackId = updateAmbariHostsRequest.getStackId();
        this.hostGroupAdjustment = updateAmbariHostsRequest.getHostGroupAdjustment();
        this.decommissionCandidates = updateAmbariHostsRequest.getDecommissionCandidates();
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public HostGroupAdjustmentJson getHostGroupAdjustment() {
        return hostGroupAdjustment;
    }

    public void setHostGroupAdjustment(HostGroupAdjustmentJson hostGroupAdjustment) {
        this.hostGroupAdjustment = hostGroupAdjustment;
    }

    public List<HostMetadata> getDecommissionCandidates() {
        return decommissionCandidates;
    }

    public void setDecommissionCandidates(List<HostMetadata> decommissionCandidates) {
        this.decommissionCandidates = decommissionCandidates;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}
