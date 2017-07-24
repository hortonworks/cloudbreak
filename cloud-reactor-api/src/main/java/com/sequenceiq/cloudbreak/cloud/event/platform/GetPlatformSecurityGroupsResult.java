package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;

public class GetPlatformSecurityGroupsResult extends CloudPlatformResult<CloudPlatformRequest> {
    private CloudSecurityGroups cloudSecurityGroups;

    public GetPlatformSecurityGroupsResult(CloudPlatformRequest<?> request, CloudSecurityGroups cloudSecurityGroups) {
        super(request);
        this.cloudSecurityGroups = cloudSecurityGroups;
    }

    public GetPlatformSecurityGroupsResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudSecurityGroups getCloudSecurityGroups() {
        return cloudSecurityGroups;
    }
}
