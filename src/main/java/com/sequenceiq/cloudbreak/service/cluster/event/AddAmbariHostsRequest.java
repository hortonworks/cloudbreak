package com.sequenceiq.cloudbreak.service.cluster.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;

public class AddAmbariHostsRequest {

    private Long stackId;
    private Set<HostGroupAdjustmentJson> hosts;

    public AddAmbariHostsRequest(Long stackId, String ambariIp, Set<HostGroupAdjustmentJson> hosts) {
        this.stackId = stackId;
        this.hosts = hosts;
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
}
