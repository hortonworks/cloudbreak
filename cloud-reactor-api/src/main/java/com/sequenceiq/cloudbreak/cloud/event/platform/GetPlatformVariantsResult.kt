package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants

class GetPlatformVariantsResult : CloudPlatformResult<CloudPlatformRequest<Any>> {
    val platformVariants: PlatformVariants

    constructor(request: CloudPlatformRequest<*>, platformVariants: PlatformVariants) : super(request) {
        this.platformVariants = platformVariants
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
