package com.sequenceiq.cloudbreak.reactor.handler.orchestration

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class StartAmbariServicesHandler : ReactorEventHandler<StartAmbariServicesRequest> {
    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val clusterServiceRunner: ClusterServiceRunner? = null

    override fun selector(): String {
        return EventSelectorUtil.selector(StartAmbariServicesRequest::class.java)
    }

    override fun accept(event: Event<StartAmbariServicesRequest>) {
        val stackId = event.data.stackId
        val response: Selectable
        try {
            clusterServiceRunner!!.runAmbariServices(stackId)
            response = StartAmbariServicesSuccess(stackId)
        } catch (e: Exception) {
            response = StartAmbariServicesFailed(stackId, e)
        }

        eventBus!!.notify(response.selector(), Event(event.headers, response))
    }
}
