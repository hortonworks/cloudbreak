package com.sequenceiq.cloudbreak.cloud.aws.status;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class AwsStackStatus {

    private AwsStackStatus() {
    }

    public static ResourceStatus mapResourceStatus(String status) {
        switch (status) {
            case "Accepted":
                return ResourceStatus.IN_PROGRESS;
            case "Ready":
                return ResourceStatus.UPDATED;
            case "Canceled":
                return ResourceStatus.FAILED;
            case "Failed":
                return ResourceStatus.FAILED;
            case "Deleted":
                return ResourceStatus.DELETED;
            case "Succeeded":
                return ResourceStatus.CREATED;
            default:
                return ResourceStatus.IN_PROGRESS;

        }
    }
}