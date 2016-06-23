package com.sequenceiq.cloudbreak.reactor.api.event.resource

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult

class ClusterSyncResult : ClusterPlatformResult<ClusterSyncRequest> {
    constructor(request: ClusterSyncRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: ClusterSyncRequest) : super(statusReason, errorDetails, request) {
    }
}
