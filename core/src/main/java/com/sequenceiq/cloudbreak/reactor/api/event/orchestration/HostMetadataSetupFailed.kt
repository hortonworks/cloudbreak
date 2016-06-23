package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

class HostMetadataSetupFailed(stackId: Long?, exception: Exception) : StackFailureEvent(stackId, exception)
