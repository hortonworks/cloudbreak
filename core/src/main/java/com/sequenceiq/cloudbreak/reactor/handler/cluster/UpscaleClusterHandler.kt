package com.sequenceiq.cloudbreak.reactor.handler.cluster

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class UpscaleClusterHandler : ClusterEventHandler<UpscaleClusterRequest> {

    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val clusterUpscaleService: AmbariClusterUpscaleService? = null

    override fun type(): Class<UpscaleClusterRequest> {
        return UpscaleClusterRequest::class.java
    }

    override fun accept(event: Event<UpscaleClusterRequest>) {
        val request = event.data
        val result: UpscaleClusterResult
        try {
            clusterUpscaleService!!.installServicesOnNewHosts(request.stackId, request.hostGroupName)
            result = UpscaleClusterResult(request)
        } catch (e: Exception) {
            result = UpscaleClusterResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
