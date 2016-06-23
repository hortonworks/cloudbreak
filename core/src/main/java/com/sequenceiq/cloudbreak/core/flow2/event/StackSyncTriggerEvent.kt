package com.sequenceiq.cloudbreak.core.flow2.event

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class StackSyncTriggerEvent(selector: String, stackId: Long?, val statusUpdateEnabled: Boolean?) : StackEvent(selector, stackId)
