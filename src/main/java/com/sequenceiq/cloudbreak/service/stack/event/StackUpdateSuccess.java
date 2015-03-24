package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

public class StackUpdateSuccess {

    private Long stackId;
    private boolean removeInstances;
    private Set<String> instanceIds;
    private String instanceGroup;
    private Boolean withClusterEvent;

    public StackUpdateSuccess(Long stackId, boolean removeInstances, Set<String> instanceIds, String instanceGroup, Boolean withClusterEvent) {
        this.stackId = stackId;
        this.removeInstances = removeInstances;
        this.instanceIds = instanceIds;
        this.instanceGroup = instanceGroup;
        this.withClusterEvent = withClusterEvent;
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

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public Boolean isWithClusterEvent() {
        return withClusterEvent;
    }
}
