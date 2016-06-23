package com.sequenceiq.cloudbreak.reactor.handler.cluster

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterCreationService
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariFailed
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class StartAmbariHandler : ReactorEventHandler<StartAmbariRequest> {
    @Inject
    private val ambariClusterCreationService: AmbariClusterCreationService? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun selector(): String {
        return EventSelectorUtil.selector(StartAmbariRequest::class.java)
    }

    override fun accept(event: Event<StartAmbariRequest>) {
        val stackId = event.data.stackId
        val response: Selectable
        try {
            ambariClusterCreationService!!.startAmbari(stackId)
            response = StartAmbariSuccess(stackId)
        } catch (e: Exception) {
            response = StartAmbariFailed(stackId, e)
        }

        eventBus!!.notify(response.selector(), Event(event.headers, response))
    }
}
