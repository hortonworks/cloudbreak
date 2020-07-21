package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;

public class GetPlatformResourceGroupsResult extends CloudPlatformResult {
    private CloudResourceGroups resourceGroups;

    public GetPlatformResourceGroupsResult(Long resourceId, CloudResourceGroups resourceGroups) {
        super(resourceId);
        this.resourceGroups = resourceGroups;
    }

    public GetPlatformResourceGroupsResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudResourceGroups getResourceGroups() {
        return resourceGroups;
    }
}
