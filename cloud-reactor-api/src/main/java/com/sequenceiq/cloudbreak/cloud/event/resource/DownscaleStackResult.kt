package com.sequenceiq.cloudbreak.cloud.event.resource

import java.util.ArrayList

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.CloudResource

class DownscaleStackResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    var downscaledResources: List<CloudResource>? = null
        private set

    constructor(request: CloudPlatformRequest<*>, downscaledResources: List<CloudResource>) : super(request) {
        this.downscaledResources = downscaledResources
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
        this.downscaledResources = ArrayList<CloudResource>()
    }
}
