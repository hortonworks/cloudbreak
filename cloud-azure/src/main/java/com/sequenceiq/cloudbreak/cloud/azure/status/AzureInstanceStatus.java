package com.sequenceiq.cloudbreak.cloud.azure.status;

import com.microsoft.azure.management.compute.PowerState;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class AzureInstanceStatus {

    private AzureInstanceStatus() {
    }

    public static InstanceStatus get(PowerState powerState) {
        if (PowerState.RUNNING.equals(powerState)) {
            return InstanceStatus.STARTED;
        } else if (PowerState.STOPPED.equals(powerState) || PowerState.DEALLOCATED.equals(powerState)) {
            return InstanceStatus.STOPPED;
        } else {
            return InstanceStatus.IN_PROGRESS;
        }
    }
}
