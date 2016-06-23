package com.sequenceiq.cloudbreak.cloud.event.setup

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult

class SetupResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    constructor(request: CloudPlatformRequest<*>) : super(request) {
    }

    constructor(errorDetails: Exception, request: CloudPlatformRequest<*>) : this(errorDetails.message, errorDetails, request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }

}
