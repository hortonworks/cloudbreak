package com.sequenceiq.cloudbreak.reactor.api.event.recipe

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest

class UpscalePostRecipesRequest(stackId: Long?, hostGroupName: String) : AbstractClusterScaleRequest(stackId, hostGroupName)
