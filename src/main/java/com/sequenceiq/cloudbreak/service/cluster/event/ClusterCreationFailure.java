package com.sequenceiq.cloudbreak.service.cluster.event;

public class ClusterCreationFailure {

    private Long stackId;
    private Long clusterId;
    private String detailedMessage;

    public ClusterCreationFailure(Long stackId, Long clusterId, String detailedMessage) {
        this.stackId = stackId;
        this.clusterId = clusterId;
        this.detailedMessage = detailedMessage;
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

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

}
