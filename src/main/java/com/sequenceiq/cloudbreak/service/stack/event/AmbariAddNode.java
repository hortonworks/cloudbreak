package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;

public class AmbariAddNode {

    private Long stackId;
    private String ambariIp;
    private Set<HostGroupJson> hosts;

    public AmbariAddNode(Long stackId, String ambariIp, Set<HostGroupJson> hosts) {
        this.stackId = stackId;
        this.ambariIp = ambariIp;
        this.hosts = hosts;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public Set<HostGroupJson> getHosts() {
        return hosts;
    }

    public void setHosts(Set<HostGroupJson> hosts) {
        this.hosts = hosts;
    }
}
