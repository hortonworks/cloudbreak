package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines

class GetVirtualMachineTypesResult : CloudPlatformResult<CloudPlatformRequest<Any>> {
    val platformVirtualMachines: PlatformVirtualMachines

    constructor(request: CloudPlatformRequest<*>, platformVirtualMachines: PlatformVirtualMachines) : super(request) {
        this.platformVirtualMachines = platformVirtualMachines
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
