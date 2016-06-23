package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class HostMetadataSetupSuccess(stackId: Long?) : StackEvent(stackId)
