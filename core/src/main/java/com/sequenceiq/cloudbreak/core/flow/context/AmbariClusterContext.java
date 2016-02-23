package com.sequenceiq.cloudbreak.core.flow.context;

public class AmbariClusterContext {

    private final Long stackId;
    private final String stackName;
    private final Long clusterId;
    private final String owner;

    public AmbariClusterContext(Long stackId, String stackName, Long clusterId, String owner) {
        this.stackId = stackId;
        this.stackName = stackName;
        this.clusterId = clusterId;
        this.owner = owner;
    }

    public Long getStackId() {
        return stackId;
    }

    public String getStackName() {
        return stackName;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public String getOwner() {
        return owner;
    }
}
