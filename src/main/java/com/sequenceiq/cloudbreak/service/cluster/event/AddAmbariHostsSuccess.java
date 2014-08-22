package com.sequenceiq.cloudbreak.service.cluster.event;

public class AddAmbariHostsSuccess {

    private Long clusterId;
    private String ambariIp;

    public AddAmbariHostsSuccess(Long clusterId, String ambariIp) {
        this.clusterId = clusterId;
        this.ambariIp = ambariIp;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }
}
