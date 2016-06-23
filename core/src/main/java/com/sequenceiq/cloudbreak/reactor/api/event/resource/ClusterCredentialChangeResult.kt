package com.sequenceiq.cloudbreak.reactor.api.event.resource

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult

class ClusterCredentialChangeResult : ClusterPlatformResult<ClusterCredentialChangeRequest> {
    constructor(request: ClusterCredentialChangeRequest) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: ClusterCredentialChangeRequest) : super(statusReason, errorDetails, request) {
    }
}
