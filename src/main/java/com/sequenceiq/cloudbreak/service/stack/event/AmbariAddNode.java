package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Map;

public class AmbariAddNode {

    private Long stackId;
    private String ambariIp;
    private Map<String, Integer> hosts;

    public AmbariAddNode(Long stackId, String ambariIp, Map<String, Integer> hosts) {
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

    public Map<String, Integer> getHosts() {
        return hosts;
    }

    public void setHosts(Map<String, Integer> hosts) {
        this.hosts = hosts;
    }
}
