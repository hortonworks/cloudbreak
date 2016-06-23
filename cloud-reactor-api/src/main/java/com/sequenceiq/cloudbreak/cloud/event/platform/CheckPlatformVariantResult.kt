package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.Variant

class CheckPlatformVariantResult : CloudPlatformResult<CloudPlatformRequest<Any>> {
    val defaultPlatformVariant: Variant

    constructor(request: CloudPlatformRequest<*>, defaultPlatformVariant: Variant) : super(request) {
        this.defaultPlatformVariant = defaultPlatformVariant
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
