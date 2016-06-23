package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class StartAmbariRequest(stackId: Long?) : StackEvent(stackId)
