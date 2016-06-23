package com.sequenceiq.cloudbreak.reactor.api.event.resource

class UpdateInstanceMetadataResult : AbstractClusterScaleResult<UpdateInstanceMetadataRequest> {

    constructor(request: UpdateInstanceMetadataRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: UpdateInstanceMetadataRequest) : super(statusReason, errorDetails, request) {
    }
}
