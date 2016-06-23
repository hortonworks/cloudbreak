package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult

class ClusterStartResult : ClusterPlatformResult<ClusterStartRequest> {
    constructor(request: ClusterStartRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: ClusterStartRequest) : super(statusReason, errorDetails, request) {
    }
}
