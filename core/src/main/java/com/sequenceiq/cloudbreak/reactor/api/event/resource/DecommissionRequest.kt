package com.sequenceiq.cloudbreak.reactor.api.event.resource

class DecommissionRequest(stackId: Long?, hostGroupName: String, val scalingAdjustment: Int?) : AbstractClusterScaleRequest(stackId, hostGroupName)
