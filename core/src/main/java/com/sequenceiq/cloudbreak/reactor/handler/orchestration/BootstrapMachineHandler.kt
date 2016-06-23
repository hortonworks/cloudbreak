package com.sequenceiq.cloudbreak.reactor.handler.orchestration

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesFailed
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class BootstrapMachineHandler : ReactorEventHandler<BootstrapMachinesRequest> {
    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val clusterBootstrapper: ClusterBootstrapper? = null

    override fun selector(): String {
        return EventSelectorUtil.selector(BootstrapMachinesRequest::class.java)
    }

    override fun accept(event: Event<BootstrapMachinesRequest>) {
        val request = event.data
        val response: Selectable
        try {
            clusterBootstrapper!!.bootstrapMachines(request.stackId)
            response = BootstrapMachinesSuccess(request.stackId)
        } catch (e: Exception) {
            response = BootstrapMachinesFailed(request.stackId, e)
        }

        eventBus!!.notify(response.selector(), Event(event.headers, response))
    }
}
