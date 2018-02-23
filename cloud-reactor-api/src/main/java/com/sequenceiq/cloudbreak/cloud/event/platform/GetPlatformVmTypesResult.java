package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;

public class GetPlatformVmTypesResult extends CloudPlatformResult<CloudPlatformRequest<?>> {
    private CloudVmTypes vmTypes;

    public GetPlatformVmTypesResult(CloudPlatformRequest<?> request, CloudVmTypes vmTypes) {
        super(request);
        this.vmTypes = vmTypes;
    }

    public GetPlatformVmTypesResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudVmTypes getCloudVmTypes() {
        return vmTypes;
    }
}
