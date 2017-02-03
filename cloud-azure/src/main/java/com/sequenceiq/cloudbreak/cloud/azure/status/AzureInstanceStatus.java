package com.sequenceiq.cloudbreak.cloud.azure.status;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public enum AzureInstanceStatus {

    STARTED("running"),
    STOPPED("stopped");

    private final String status;

    AzureInstanceStatus(String status) {
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
