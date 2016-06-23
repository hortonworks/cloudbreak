package com.sequenceiq.cloudbreak.core.flow2.event

import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

open class ClusterScaleTriggerEvent(selector: String, stackId: Long?, override val hostGroupName: String, val adjustment: Int?) : StackEvent(selector, stackId), HostGroupPayload
