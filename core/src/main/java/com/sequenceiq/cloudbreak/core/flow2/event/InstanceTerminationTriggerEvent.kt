package com.sequenceiq.cloudbreak.core.flow2.event

import com.sequenceiq.cloudbreak.cloud.event.InstancePayload
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class InstanceTerminationTriggerEvent(selector: String, stackId: Long?, override val instanceId: String) : StackEvent(selector, stackId), InstancePayload
