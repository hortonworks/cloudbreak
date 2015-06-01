package com.sequenceiq.cloudbreak.cloud.event.context;

public class StackContext {

    private long stackId;

    private String stackName;

    private String platform;

    public StackContext(long stackId, String stackName, String platform) {
        this.stackId = stackId;
        this.stackName = stackName;
        this.platform = platform;
    }

    public long getStackId() {
        return stackId;
    }

    public String getStackName() {
        return stackName;
    }

    public String getPlatform() {
        return platform;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "StackContext{" +
                "stackId=" + stackId +
                ", stackName='" + stackName + '\'' +
                ", platform='" + platform + '\'' +
                '}';
    }
    //END GENERATED CODE
}
