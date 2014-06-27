package com.sequenceiq.cloudbreak.service.cluster;

public class ClusterCreationSuccess {

    private Long clusterId;
    private long creationFinished;

    public ClusterCreationSuccess(Long clusterId, long creationFinished) {
        this.clusterId = clusterId;
        this.creationFinished = creationFinished;
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
}
