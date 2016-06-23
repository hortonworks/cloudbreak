package com.sequenceiq.cloudbreak.cloud.transform

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult

object ResourcesStatePollerResults {

    fun build(context: CloudContext, results: MutableList<CloudResourceStatus>): ResourcesStatePollerResult {
        val status = ResourceStatusLists.aggregate(results)
        return ResourcesStatePollerResult(context, status.status, status.statusReason, results)
    }

    fun transformToLaunchStackResult(request: LaunchStackRequest, result: ResourcesStatePollerResult): LaunchStackResult {
        return LaunchStackResult(request, result.results)
    }

    fun transformToUpscaleStackResult(result: ResourcesStatePollerResult, request: UpscaleStackRequest<Any>): UpscaleStackResult {
        return UpscaleStackResult(request, result.status, result.results)
    }
}
