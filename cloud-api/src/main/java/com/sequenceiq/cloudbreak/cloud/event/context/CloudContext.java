package com.sequenceiq.cloudbreak.cloud.event.context;

import com.sequenceiq.cloudbreak.domain.Stack;

public class CloudContext {

    private Long stackId;
    private String stackName;
    private String platform;
    private int parallelResourceRequest;
    private String owner;
    private String region;

    public CloudContext(Stack stack) {
        this.stackId = stack.getId();
        this.stackName = stack.getName();
        this.platform = stack.cloudPlatform().name();
        this.region = stack.getRegion();
        this.owner = stack.getOwner();
        this.parallelResourceRequest = stack.cloudPlatform().parallelNumber();
    }

    public CloudContext(Long stackId, String stackName, String platform, String owner) {
        this.stackId = stackId;
        this.stackName = stackName;
        this.platform = platform;
        this.owner = owner;
    }

    public Long getStackId() {
        return stackId;
    }

    public String getStackName() {
        return stackName;
    }

    public String getPlatform() {
        return platform;
    }

    public String getOwner() {
        return owner;
    }

    public String getRegion() {
        return region;
    }

    public int getParallelResourceRequest() {
        return parallelResourceRequest;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudContext{");
        sb.append("stackId=").append(stackId);
        sb.append(", stackName='").append(stackName).append('\'');
        sb.append(", platform='").append(platform).append('\'');
        sb.append(", parallelResourceAction=").append(parallelResourceRequest);
        sb.append(", region='").append(region).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
