package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest

class UpscaleClusterRequest(stackId: Long?, hostGroupName: String) : AbstractClusterScaleRequest(stackId, hostGroupName)
