package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;

public class AmbariAddNode {

    private Long stackId;
    private Set<HostGroupAdjustmentJson> hosts;

    public AmbariAddNode(Long stackId, String ambariIp, Set<HostGroupAdjustmentJson> hosts) {
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
