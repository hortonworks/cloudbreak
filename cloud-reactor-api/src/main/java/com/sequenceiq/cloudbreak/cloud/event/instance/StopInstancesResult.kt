package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.event.Payload

class StopInstancesResult : CloudPlatformResult<StopInstancesRequest<Any>>, Payload {

    var cloudContext: CloudContext? = null
        private set
    val results: InstancesStatusResult

    constructor(request: StopInstancesRequest<Any>, cloudContext: CloudContext, results: InstancesStatusResult) : super(request) {
        this.cloudContext = cloudContext
        this.results = results
    }

    constructor(statusReason: String, errorDetails: Exception, request: StopInstancesRequest<Any>) : super(statusReason, errorDetails, request) {
        this.cloudContext = request.cloudContext
    }

    override val stackId: Long?
        get() = cloudContext!!.id

}
