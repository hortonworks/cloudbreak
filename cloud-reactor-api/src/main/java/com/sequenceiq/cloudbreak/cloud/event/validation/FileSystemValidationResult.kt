package com.sequenceiq.cloudbreak.cloud.event.validation

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult

class FileSystemValidationResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    constructor(request: CloudPlatformRequest<Any>) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<Any>) : super(statusReason, errorDetails, request) {
    }
}
