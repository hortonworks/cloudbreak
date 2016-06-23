package com.sequenceiq.cloudbreak.cloud.event.resource

import java.util.ArrayList

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus

class UpscaleStackResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    var resourceStatus: ResourceStatus? = null
        private set
    var results: List<CloudResourceStatus>? = null
        private set

    constructor(request: CloudPlatformRequest<*>, resourceStatus: ResourceStatus, results: List<CloudResourceStatus>) : super(request) {
        this.resourceStatus = resourceStatus
        this.results = results
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
        this.resourceStatus = ResourceStatus.FAILED
        this.results = ArrayList<CloudResourceStatus>()
    }

    val isFailed: Boolean
        get() = resourceStatus === ResourceStatus.FAILED

}
