package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult

class ClusterStopResult : ClusterPlatformResult<ClusterStopRequest> {
    constructor(request: ClusterStopRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: ClusterStopRequest) : super(statusReason, errorDetails, request) {
    }
}
