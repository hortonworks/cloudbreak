package com.sequenceiq.cloudbreak.reactor.api.event.recipe

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult

class UpscalePreRecipesResult : AbstractClusterScaleResult<UpscalePreRecipesRequest> {

    constructor(request: UpscalePreRecipesRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: UpscalePreRecipesRequest) : super(statusReason, errorDetails, request) {
    }
}
