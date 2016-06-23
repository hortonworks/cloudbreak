package com.sequenceiq.cloudbreak.reactor.api.event.resource

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest

abstract class AbstractClusterBootstrapRequest(stackId: Long?, val upscaleCandidateAddresses: Set<String>) : ClusterPlatformRequest(stackId)
