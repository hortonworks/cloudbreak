package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

class InstallClusterFailed(stackId: Long?, ex: Exception) : StackFailureEvent(stackId, ex)
