package com.sequenceiq.cloudbreak.cloud.openstack.status;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class HeatStackStatus {


    public static ResourceStatus mapResourceStatus(String status) {
        if (Strings.isNullOrEmpty(status) || status.contains("FAILED")) {
            return ResourceStatus.FAILED;
        }

        switch (status) {
            case "CREATE_COMPLETE":
                return ResourceStatus.CREATED;
            case "DELETE_COMPLETE":
                return ResourceStatus.DELETED;
            case "UPDATE_COMPLETE":
                return ResourceStatus.UPDATED;
            default:
                return ResourceStatus.IN_PROGRESS;

        }

    }
}
