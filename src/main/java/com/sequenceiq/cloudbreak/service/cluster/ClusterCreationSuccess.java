package com.sequenceiq.cloudbreak.service.cluster;

public class ClusterCreationSuccess {

    private Long clusterId;
    private long creationFinished;
    private String ambariIp;

    public ClusterCreationSuccess(Long clusterId, long creationFinished, String ambariIp) {
        this.clusterId = clusterId;
        this.creationFinished = creationFinished;
        this.ambariIp = ambariIp;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public long getCreationFinished() {
        return creationFinished;
    }

    public void setCreationFinished(long creationFinished) {
        this.creationFinished = creationFinished;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }
}
