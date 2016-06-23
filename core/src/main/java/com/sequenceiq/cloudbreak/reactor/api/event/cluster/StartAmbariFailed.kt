package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

class StartAmbariFailed(stackId: Long?, ex: Exception) : StackFailureEvent(stackId, ex)
