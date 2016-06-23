package com.sequenceiq.cloudbreak.cloud.event.resource

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus

class LaunchStackResult : CloudPlatformResult<CloudPlatformRequest<Any>> {
    val results: List<CloudResourceStatus>

    constructor(request: CloudPlatformRequest<*>, results: List<CloudResourceStatus>) : super(request) {
        this.results = results
    }

    constructor(errorDetails: Exception, request: CloudPlatformRequest<*>) : super("", errorDetails, request) {
    }

    override fun toString(): String {
        return "LaunchStackResult{"
        +"status=" + status
        +", statusReason='" + statusReason + '\''
        +", errorDetails=" + errorDetails
        +", request=" + request
        +", results=" + results
        +'}'
    }
}
