package com.sequenceiq.cloudbreak.cloud.cumulus.yarn.util;

import org.apache.cb.yarn.service.api.records.ContainerState;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class CumulusYarnContainerStatus {
    private CumulusYarnContainerStatus() {
    }

    public static InstanceStatus mapInstanceStatus(ContainerState state) {
        switch (state.getState()) {
            case INIT:
                return InstanceStatus.CREATE_REQUESTED;
            case STARTED:
                return InstanceStatus.CREATED;
            case READY:
                return InstanceStatus.STARTED;
            default:
                return InstanceStatus.UNKNOWN;
        }
    }
}
