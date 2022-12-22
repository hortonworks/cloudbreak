package com.sequenceiq.cloudbreak.cloud.gcp.service.naming;

public class GcpInstanceGroupResourceName {

    private final String stackName;

    private final String groupName;

    private final String suffix;

    public GcpInstanceGroupResourceName(String stackName, String groupName, String suffix) {
        this.stackName = stackName;
        this.groupName = groupName;
        this.suffix = suffix;
    }

    public String getStackName() {
        return stackName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public String toString() {
        return "GcpInstanceGroupResourceName{" +
                "stackName='" + stackName + '\'' +
                ", groupName='" + groupName + '\'' +
                ", suffix='" + suffix + '\'' +
                '}';
    }
}