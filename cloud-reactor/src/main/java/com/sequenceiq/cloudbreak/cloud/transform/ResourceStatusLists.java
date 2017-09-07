package com.sequenceiq.cloudbreak.cloud.transform;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class ResourceStatusLists {
    private ResourceStatusLists() {
    }

    public static CloudResourceStatus aggregate(List<CloudResourceStatus> cloudResourceStatuses) {

        ResourceStatus status = null;
        StringBuilder statusReason = new StringBuilder();

        for (CloudResourceStatus crs : cloudResourceStatuses) {
            ResourceStatus currentStatus = crs.getStatus();

            if (status == null) {
                status = currentStatus;
            }

            switch (currentStatus) {
                case FAILED:
                    status = currentStatus;
                    statusReason.append(crs.getStatusReason()).append('\n');
                    break;
                default:
                    if (currentStatus.isTransient()) {
                        status = currentStatus;
                    }
            }
        }

        if (status == null) {
            status = ResourceStatus.FAILED;
            statusReason.append("Resources does not have any state");
        }


        return new CloudResourceStatus(null, status, statusReason.toString());
    }


}
