package com.sequenceiq.cloudbreak.cloud.cumulus.yarn;

import org.apache.cb.yarn.service.api.records.ServiceState;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class CumulusYarnApplicationStatus {
    private CumulusYarnApplicationStatus() {
    }

    public static ResourceStatus mapResourceStatus(ServiceState state) {
        if (state == null) {
            return ResourceStatus.FAILED;
        }

        switch (state.getState()) {
            case ACCEPTED:
            case STARTED:
            case STOPPED:
                return ResourceStatus.IN_PROGRESS;
            case STABLE:
                return ResourceStatus.CREATED;
            default:
                return ResourceStatus.IN_PROGRESS;
        }
    }
}
