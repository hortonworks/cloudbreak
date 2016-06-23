package com.sequenceiq.cloudbreak.reactor.api.event.recipe

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest

class UpscalePreRecipesRequest(stackId: Long?, hostGroupName: String) : AbstractClusterScaleRequest(stackId, hostGroupName)
