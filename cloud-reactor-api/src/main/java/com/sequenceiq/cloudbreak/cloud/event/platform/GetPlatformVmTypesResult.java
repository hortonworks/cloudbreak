package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;

public class GetPlatformVmTypesResult extends CloudPlatformResult {
    private CloudVmTypes vmTypes;

    public GetPlatformVmTypesResult(Long resourceId, CloudVmTypes vmTypes) {
        super(resourceId);
        this.vmTypes = vmTypes;
    }

    public GetPlatformVmTypesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudVmTypes getCloudVmTypes() {
        return vmTypes;
    }
}
