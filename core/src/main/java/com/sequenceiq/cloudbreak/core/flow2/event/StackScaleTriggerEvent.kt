package com.sequenceiq.cloudbreak.core.flow2.event

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

open class StackScaleTriggerEvent(selector: String, stackId: Long?, val instanceGroup: String, val adjustment: Int?) : StackEvent(selector, stackId)
