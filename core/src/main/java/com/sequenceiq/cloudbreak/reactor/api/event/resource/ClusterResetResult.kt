package com.sequenceiq.cloudbreak.reactor.api.event.resource

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult

class ClusterResetResult : ClusterPlatformResult<ClusterResetRequest> {
    constructor(request: ClusterResetRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: ClusterResetRequest) : super(statusReason, errorDetails, request) {
    }
}
