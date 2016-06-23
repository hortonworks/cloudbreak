package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult

class UpscaleAmbariResult : AbstractClusterScaleResult<UpscaleAmbariRequest> {

    constructor(request: UpscaleAmbariRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: UpscaleAmbariRequest) : super(statusReason, errorDetails, request) {
    }
}
