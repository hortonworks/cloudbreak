package com.sequenceiq.cloudbreak.core.flow2.stack.upscale

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class BootstrapNewNodesEvent(stackId: Long?, val upscaleCandidateAddresses: Set<String>) : StackEvent(stackId)
