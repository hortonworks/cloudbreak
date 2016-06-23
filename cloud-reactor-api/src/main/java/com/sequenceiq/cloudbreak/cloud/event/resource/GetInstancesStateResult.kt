package com.sequenceiq.cloudbreak.cloud.event.resource

import java.util.Collections

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus

class GetInstancesStateResult : CloudPlatformResult<GetInstancesStateRequest<GetInstancesStateResult>> {

    val statuses: List<CloudVmInstanceStatus>

    constructor(request: GetInstancesStateRequest<GetInstancesStateResult>) : super(request) {
        this.statuses = emptyList<CloudVmInstanceStatus>()
    }

    constructor(request: GetInstancesStateRequest<GetInstancesStateResult>, statuses: List<CloudVmInstanceStatus>) : super(request) {
        this.statuses = statuses
    }

    constructor(statusReason: String, errorDetails: Exception, request: GetInstancesStateRequest<GetInstancesStateResult>) : super(statusReason, errorDetails, request) {
        this.statuses = emptyList<CloudVmInstanceStatus>()
    }

    val cloudContext: CloudContext
        get() = request.cloudContext

    val isFailed: Boolean
        get() = errorDetails != null

    override fun toString(): String {
        val sb = StringBuilder("GetInstancesStateResult{")
        sb.append("cloudContext=").append(cloudContext)
        sb.append(", statuses=").append(statuses)
        sb.append(", exception=").append(errorDetails)
        sb.append('}')
        return sb.toString()
    }
}
