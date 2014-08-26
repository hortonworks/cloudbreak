package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

public class StackUpdateSuccess {

    private Long stackId;
    private boolean removeInstances;
    private Set<String> instanceIds;

    public StackUpdateSuccess(Long stackId, boolean removeInstances, Set<String> instanceIds) {
        this.stackId = stackId;
        this.removeInstances = removeInstances;
        this.instanceIds = instanceIds;
    }

    public Long getStackId() {
        return stackId;
    }

    public boolean isRemoveInstances() {
        return removeInstances;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

}
