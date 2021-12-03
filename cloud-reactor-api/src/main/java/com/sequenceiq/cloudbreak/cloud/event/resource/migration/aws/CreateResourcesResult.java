package com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;

public class CreateResourcesResult extends CloudPlatformResult {

    public CreateResourcesResult(Long resourceId) {
        super(resourceId);
    }

    public CreateResourcesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CreateResourcesResult(EventStatus status, String statusReason, Exception errorDetails, Long resourceId) {
        super(status, statusReason, errorDetails, resourceId);
    }
}
