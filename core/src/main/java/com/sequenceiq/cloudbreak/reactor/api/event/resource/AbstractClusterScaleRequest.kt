package com.sequenceiq.cloudbreak.reactor.api.event.resource

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload

abstract class AbstractClusterScaleRequest(stackId: Long?, override val hostGroupName: String) : ClusterPlatformRequest(stackId), HostGroupPayload
