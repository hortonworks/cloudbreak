package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult

class ClusterTerminationResult : ClusterPlatformResult<ClusterTerminationRequest> {
    constructor(request: ClusterTerminationRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: ClusterTerminationRequest) : super(statusReason, errorDetails, request) {
    }
}
