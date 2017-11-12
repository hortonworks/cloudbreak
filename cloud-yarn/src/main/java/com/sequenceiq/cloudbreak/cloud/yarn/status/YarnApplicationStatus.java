package com.sequenceiq.cloudbreak.cloud.yarn.status;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.ApplicationState;

public class YarnApplicationStatus {
    private YarnApplicationStatus() {
    }

    public static ResourceStatus mapResourceStatus(String status) {
        return mapResourceStatus(getApplicationState(status));
    }

    public static ResourceStatus mapResourceStatus(ApplicationState status) {
        if (status == null) {
            return ResourceStatus.FAILED;
        }

        switch (status) {
            case ACCEPTED:
            case STARTED:
            case STOPPED:
                return ResourceStatus.IN_PROGRESS;
            case READY:
                return ResourceStatus.CREATED;
            default:
                return ResourceStatus.IN_PROGRESS;

        }
    }

    private static ApplicationState getApplicationState(String status) {
        try {
            return ApplicationState.valueOf(status);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
