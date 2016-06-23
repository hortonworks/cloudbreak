package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest

class UpscaleAmbariRequest(stackId: Long?, hostGroupName: String, val scalingAdjustment: Int?) : AbstractClusterScaleRequest(stackId, hostGroupName)
