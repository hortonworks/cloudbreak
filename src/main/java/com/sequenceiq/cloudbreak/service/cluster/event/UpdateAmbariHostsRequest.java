package com.sequenceiq.cloudbreak.service.cluster.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;

public class UpdateAmbariHostsRequest {

    private Long stackId;
    private Set<HostGroupAdjustmentJson> hosts;
    private boolean decommision;

    public UpdateAmbariHostsRequest(Long stackId, Set<HostGroupAdjustmentJson> hosts, boolean decommision) {
        this.stackId = stackId;
        this.hosts = hosts;
        this.decommision = decommision;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Set<HostGroupAdjustmentJson> getHosts() {
        return hosts;
    }

    public void setHosts(Set<HostGroupAdjustmentJson> hosts) {
        this.hosts = hosts;
    }

    public boolean isDecommision() {
        return decommision;
    }

    public void setDecommision(boolean decommision) {
        this.decommision = decommision;
    }
}
