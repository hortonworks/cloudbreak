package com.sequenceiq.cloudbreak.service.cluster.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class ClusterDeleteRequest {
    private Long stackId;
    private Platform cloudPlatform;
    private Long clusterId;

    public ClusterDeleteRequest(Long stackId, Platform cloudPlatform, Long clusterId) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
        this.clusterId = clusterId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Platform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(Platform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }
}
