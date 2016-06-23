package com.sequenceiq.cloudbreak.reactor.api.event.resource

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult

abstract class AbstractClusterBootstrapResult<R : AbstractClusterBootstrapRequest> : ClusterPlatformResult<R> {
    constructor(request: R) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: R) : super(statusReason, errorDetails, request) {
    }
}
