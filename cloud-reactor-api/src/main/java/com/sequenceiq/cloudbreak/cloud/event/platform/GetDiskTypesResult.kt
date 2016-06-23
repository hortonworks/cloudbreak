package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks

class GetDiskTypesResult : CloudPlatformResult<CloudPlatformRequest<Any>> {
    val platformDisks: PlatformDisks

    constructor(request: CloudPlatformRequest<*>, platformDisks: PlatformDisks) : super(request) {
        this.platformDisks = platformDisks
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
