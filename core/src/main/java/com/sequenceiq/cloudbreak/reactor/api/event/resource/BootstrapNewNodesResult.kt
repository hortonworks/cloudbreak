package com.sequenceiq.cloudbreak.reactor.api.event.resource

class BootstrapNewNodesResult : AbstractClusterBootstrapResult<BootstrapNewNodesRequest> {
    constructor(request: BootstrapNewNodesRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: BootstrapNewNodesRequest) : super(statusReason, errorDetails, request) {
    }
}
