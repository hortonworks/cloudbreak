package com.sequenceiq.cloudbreak.service.stack.event;


import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Resource;

public class StackOperationFailure {

    private Long stackId;
    private String detailedMessage;
    private Set<Resource> resourceSet;

    public StackOperationFailure(Long stackId, String detailedMessage, Set<Resource> resourceSet) {
        this.stackId = stackId;
        this.detailedMessage = detailedMessage;
        this.resourceSet = resourceSet;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    public Set<Resource> getResourceSet() {
        return resourceSet;
    }

    public void setResourceSet(Set<Resource> resourceSet) {
        this.resourceSet = resourceSet;
    }
}
