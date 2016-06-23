package com.sequenceiq.cloudbreak.reactor.api.event.resource

import java.util.Collections

class DecommissionResult : AbstractClusterScaleResult<DecommissionRequest> {

    val hostNames: Set<String>

    constructor(request: DecommissionRequest, hostNames: Set<String>) : super(request) {
        this.hostNames = hostNames
    }

    constructor(statusReason: String, errorDetails: Exception, request: DecommissionRequest) : super(statusReason, errorDetails, request) {
        this.hostNames = emptySet<String>()
    }
}
