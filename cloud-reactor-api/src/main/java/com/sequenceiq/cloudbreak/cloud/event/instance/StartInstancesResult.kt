package com.sequenceiq.cloudbreak.cloud.event.instance


import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.event.Payload

class StartInstancesResult : CloudPlatformResult<StartInstancesRequest>, Payload {

    var cloudContext: CloudContext? = null
        private set
    val results: InstancesStatusResult

    constructor(request: StartInstancesRequest, cloudContext: CloudContext, results: InstancesStatusResult) : super(request) {
        this.cloudContext = cloudContext
        this.results = results
    }

    constructor(statusReason: String, errorDetails: Exception, request: StartInstancesRequest) : super(statusReason, errorDetails, request) {
        this.cloudContext = request.cloudContext
    }

    override val stackId: Long?
        get() = cloudContext!!.id

}
