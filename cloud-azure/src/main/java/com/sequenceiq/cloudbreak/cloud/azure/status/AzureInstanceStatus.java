package com.sequenceiq.cloudbreak.cloud.azure.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.management.compute.PowerState;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class AzureInstanceStatus {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceStatus.class);

    private AzureInstanceStatus() {
    }

    public static InstanceStatus get(PowerState powerState) {
        LOGGER.debug("Powerstate is: {}", powerState);
        if (PowerState.RUNNING.equals(powerState)) {
            return InstanceStatus.STARTED;
        } else if (PowerState.STOPPED.equals(powerState) || PowerState.DEALLOCATED.equals(powerState)) {
            return InstanceStatus.STOPPED;
        } else {
            return InstanceStatus.IN_PROGRESS;
        }
    }
}
