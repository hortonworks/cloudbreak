package com.sequenceiq.cloudbreak.service.cluster.event;

public class UpdateAmbariHostsFailure {

    private Long clusterId;
    private String detailedMessage;
    private boolean addingNodes;

    public UpdateAmbariHostsFailure(Long clusterId, String detailedMessage, boolean addingNodes) {
        this.clusterId = clusterId;
        this.detailedMessage = detailedMessage;
        this.addingNodes = addingNodes;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public boolean isAddingNodes() {
        return addingNodes;
    }
}
