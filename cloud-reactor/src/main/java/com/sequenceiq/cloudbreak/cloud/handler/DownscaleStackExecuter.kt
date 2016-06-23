package com.sequenceiq.cloudbreak.cloud.handler

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult

internal interface DownscaleStackExecuter {
    fun execute(request: DownscaleStackRequest<Any>): DownscaleStackResult
}
