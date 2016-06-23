package com.sequenceiq.cloudbreak.reactor.api.event.resource

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload

abstract class AbstractClusterScaleResult<R : AbstractClusterScaleRequest> : ClusterPlatformResult<R>, HostGroupPayload {

    constructor(request: R) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: R) : super(statusReason, errorDetails, request) {
    }

    override val hostGroupName: String
        get() = request.hostGroupName
}
