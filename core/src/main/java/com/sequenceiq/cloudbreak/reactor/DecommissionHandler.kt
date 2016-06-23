package com.sequenceiq.cloudbreak.reactor

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterDownscaleService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class DecommissionHandler : ClusterEventHandler<DecommissionRequest> {

    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val clusterDownscaleService: ClusterDownscaleService? = null

    override fun type(): Class<DecommissionRequest> {
        return DecommissionRequest::class.java
    }

    override fun accept(event: Event<DecommissionRequest>) {
        val request = event.data
        val result: DecommissionResult
        try {
            val hostNames = clusterDownscaleService!!.decommission(request.stackId, request.hostGroupName, request.scalingAdjustment)
            result = DecommissionResult(request, hostNames)
        } catch (e: Exception) {
            result = DecommissionResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
