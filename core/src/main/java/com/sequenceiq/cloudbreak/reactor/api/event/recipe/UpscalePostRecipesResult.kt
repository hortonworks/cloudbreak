package com.sequenceiq.cloudbreak.reactor.api.event.recipe

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult

class UpscalePostRecipesResult : AbstractClusterScaleResult<UpscalePostRecipesRequest> {

    constructor(request: UpscalePostRecipesRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: UpscalePostRecipesRequest) : super(statusReason, errorDetails, request) {
    }
}
