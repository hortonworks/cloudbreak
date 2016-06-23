package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class StartAmbariServicesRequest(stackId: Long?) : StackEvent(stackId)
