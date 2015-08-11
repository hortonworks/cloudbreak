package com.sequenceiq.cloudbreak.cloud.event.context;

public class CloudContext {

    private Long stackId;
    private String stackName;
    private String platform;

    public CloudContext(Long stackId, String stackName, String platform) {
        this.stackId = stackId;
        this.stackName = stackName;
        this.platform = platform;
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

    @Override
    public String toString() {
        return "StackContext{"
                + "stackId=" + stackId
                + ", stackName='" + stackName + '\''
                + ", platform='" + platform + '\''
                + '}';
    }
}
