package com.sequenceiq.cloudbreak.reactor

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class BootstrapNewNodesHandler : ClusterEventHandler<BootstrapNewNodesRequest> {
    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val clusterBootstrapper: ClusterBootstrapper? = null

    override fun type(): Class<BootstrapNewNodesRequest> {
        return BootstrapNewNodesRequest::class.java
    }

    override fun accept(event: Event<BootstrapNewNodesRequest>) {
        val request = event.data
        val result: BootstrapNewNodesResult
        try {
            clusterBootstrapper!!.bootstrapNewNodes(request.stackId, request.upscaleCandidateAddresses)
            result = BootstrapNewNodesResult(request)
        } catch (e: Exception) {
            result = BootstrapNewNodesResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
