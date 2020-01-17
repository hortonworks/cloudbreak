package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;

public class GetPlatformSecurityGroupsResult extends CloudPlatformResult {
    private CloudSecurityGroups cloudSecurityGroups;

    public GetPlatformSecurityGroupsResult(Long resourceId, CloudSecurityGroups cloudSecurityGroups) {
        super(resourceId);
        this.cloudSecurityGroups = cloudSecurityGroups;
    }

    public GetPlatformSecurityGroupsResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public GetPlatformSecurityGroupsResult(EventStatus status, String statusReason, Exception errorDetails, Long resourceId) {
        super(status, statusReason, errorDetails, resourceId);
    }

    public CloudSecurityGroups getCloudSecurityGroups() {
        return cloudSecurityGroups;
    }
}
