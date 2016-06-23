package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult

class PlatformParameterResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    val platformParameters: PlatformParameters

    constructor(request: CloudPlatformRequest<*>, platformParameters: PlatformParameters) : super(request) {
        this.platformParameters = platformParameters
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
