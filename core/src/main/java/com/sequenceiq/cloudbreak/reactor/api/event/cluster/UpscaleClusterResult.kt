package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult

class UpscaleClusterResult : AbstractClusterScaleResult<UpscaleClusterRequest> {

    constructor(request: UpscaleClusterRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: UpscaleClusterRequest) : super(statusReason, errorDetails, request) {
    }
}
