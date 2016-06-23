package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest

class ClusterTerminationRequest(stackId: Long?, val clusterId: Long?) : ClusterPlatformRequest(stackId)
