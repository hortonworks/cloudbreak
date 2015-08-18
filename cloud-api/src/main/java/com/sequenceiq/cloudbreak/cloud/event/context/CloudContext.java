package com.sequenceiq.cloudbreak.cloud.event.context;

public class CloudContext {

    private Long stackId;
    private String stackName;
    private String platform;
    private String owner;

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

    @Override
    public String toString() {
        return "StackContext{"
                + "stackId=" + stackId
                + ", stackName='" + stackName + '\''
                + ", platform='" + platform + '\''
                + ", owner='" + owner + '\''
                + '}';
    }
}
