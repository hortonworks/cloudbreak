package com.sequenceiq.cloudbreak.reactor.api

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil

abstract class ClusterPlatformRequest(override val stackId: Long?) : Payload, Selectable {

    override fun selector(): String {
        return EventSelectorUtil.selector(javaClass)
    }
}
