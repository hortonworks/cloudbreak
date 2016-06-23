package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators

class GetPlatformOrchestratorsResult : CloudPlatformResult<CloudPlatformRequest<Any>> {
    val platformOrchestrators: PlatformOrchestrators

    constructor(request: CloudPlatformRequest<*>, platformOrchestrators: PlatformOrchestrators) : super(request) {
        this.platformOrchestrators = platformOrchestrators
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
