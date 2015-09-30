package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;

public class GetVirtualMachineTypesResult extends CloudPlatformResult {
    private PlatformVirtualMachines platformVirtualMachines;

    public GetVirtualMachineTypesResult(CloudPlatformRequest<?> request, PlatformVirtualMachines platformVirtualMachines) {
        super(request);
        this.platformVirtualMachines = platformVirtualMachines;
    }

    public GetVirtualMachineTypesResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public PlatformVirtualMachines getPlatformVirtualMachines() {
        return platformVirtualMachines;
    }
}
