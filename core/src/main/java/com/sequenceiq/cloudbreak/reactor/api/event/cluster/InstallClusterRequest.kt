package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class InstallClusterRequest(stackId: Long?) : StackEvent(stackId)
