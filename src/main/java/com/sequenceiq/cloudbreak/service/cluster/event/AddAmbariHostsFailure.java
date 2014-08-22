package com.sequenceiq.cloudbreak.service.cluster.event;

public class AddAmbariHostsFailure {

    private Long clusterId;
    private String detailedMessage;

    public AddAmbariHostsFailure(Long clusterId, String detailedMessage) {
        this.clusterId = clusterId;
        this.detailedMessage = detailedMessage;
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
