package com.sequenceiq.cloudbreak.reactor.api.event.cluster

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class StartAmbariSuccess(stackId: Long?) : StackEvent(stackId)
