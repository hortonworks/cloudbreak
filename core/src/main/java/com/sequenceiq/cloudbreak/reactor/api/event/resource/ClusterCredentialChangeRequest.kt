package com.sequenceiq.cloudbreak.reactor.api.event.resource

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest

class ClusterCredentialChangeRequest(stackId: Long?, val user: String, val password: String) : ClusterPlatformRequest(stackId)
