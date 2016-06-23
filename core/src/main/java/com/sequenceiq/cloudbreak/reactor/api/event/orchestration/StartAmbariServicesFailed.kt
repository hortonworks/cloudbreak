package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

class StartAmbariServicesFailed(stackId: Long?, ex: Exception) : StackFailureEvent(stackId, ex)
