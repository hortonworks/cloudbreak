package com.sequenceiq.cloudbreak.reactor.api.event.resource

class ExtendHostMetadataResult : AbstractClusterBootstrapResult<ExtendHostMetadataRequest> {
    constructor(request: ExtendHostMetadataRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: ExtendHostMetadataRequest) : super(statusReason, errorDetails, request) {
    }
}
