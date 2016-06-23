package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class BootstrapMachinesRequest(stackId: Long?) : StackEvent(stackId)
