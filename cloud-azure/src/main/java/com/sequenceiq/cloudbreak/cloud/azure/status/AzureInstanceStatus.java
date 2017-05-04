package com.sequenceiq.cloudbreak.cloud.azure.status;

import com.microsoft.azure.management.compute.PowerState;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public enum AzureInstanceStatus {

    STARTED,
    STOPPED;

    public static InstanceStatus get(PowerState powerState) {
        switch (powerState) {
            case RUNNING:
                return InstanceStatus.STARTED;
            case DEALLOCATED:
                return InstanceStatus.STOPPED;
            default:
                return InstanceStatus.IN_PROGRESS;
        }
    }
}
