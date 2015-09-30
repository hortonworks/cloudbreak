package com.sequenceiq.cloudbreak.cloud.aws.status;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public enum AwsInstanceStatus {

    STARTED("running"),
    STOPPED("stopped");

    private final String status;

    private AwsInstanceStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static InstanceStatus get(String status) {
        switch (status) {
            case "stopped":
                return InstanceStatus.STOPPED;
            case "running":
                return InstanceStatus.STARTED;
            default:
                return InstanceStatus.IN_PROGRESS;
        }
    }
}
