package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions

class GetPlatformRegionsResult : CloudPlatformResult<CloudPlatformRequest<Any>> {
    val platformRegions: PlatformRegions

    constructor(request: CloudPlatformRequest<*>, platformRegions: PlatformRegions) : super(request) {
        this.platformRegions = platformRegions
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
