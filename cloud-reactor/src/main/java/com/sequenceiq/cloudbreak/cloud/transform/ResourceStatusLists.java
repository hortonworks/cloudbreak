package com.sequenceiq.cloudbreak.cloud.transform;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class ResourceStatusLists {
    private ResourceStatusLists() {
    }

    public static CloudResourceStatus aggregate(Iterable<CloudResourceStatus> cloudResourceStatuses) {

        ResourceStatus status = null;
        StringBuilder statusReason = new StringBuilder();

        for (CloudResourceStatus crs : cloudResourceStatuses) {
            ResourceStatus currentStatus = crs.getStatus();

            if (status == null) {
                status = currentStatus;
            }

            if (currentStatus == ResourceStatus.FAILED) {
                status = currentStatus;
                statusReason.append(crs.getStatusReason()).append('\n');
            } else {
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

    public static String aggregateReason(Collection<CloudResourceStatus> cloudResourceStatuses) {
        String joinedReasons = cloudResourceStatuses.stream()
                .map(CloudResourceStatus::getStatusReason)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(", "));
        return StringUtils.isBlank(joinedReasons) ? "Unknown" : joinedReasons;
    }
}
