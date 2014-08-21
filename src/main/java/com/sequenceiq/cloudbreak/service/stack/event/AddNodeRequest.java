package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class AddNodeRequest extends ProvisionEvent {

    private Integer nodeCount;

    public AddNodeRequest(CloudPlatform cloudPlatform, Long stackId, Integer nodeCount) {
        super(cloudPlatform, stackId);
        this.nodeCount = nodeCount;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

}
