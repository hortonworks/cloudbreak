package com.sequenceiq.cloudbreak.cloud.event.setup

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.common.type.ImageStatus

class CheckImageResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    val imageStatus: ImageStatus
    val statusProgressValue: Int?

    constructor(request: CloudPlatformRequest<*>, imageStatus: ImageStatus, statusProgressValue: Int?) : super(request) {
        this.imageStatus = imageStatus
        this.statusProgressValue = statusProgressValue
    }

    constructor(errorDetails: Exception, request: CloudPlatformRequest<*>, imageStatus: ImageStatus) : this(errorDetails.message, errorDetails, request, imageStatus) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>, imageStatus: ImageStatus) : super(statusReason, errorDetails, request) {
        this.imageStatus = imageStatus
        this.statusProgressValue = null
    }
}
