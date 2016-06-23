package com.sequenceiq.cloudbreak.reactor.handler.cluster

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariResult
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class UpscaleAmbariHandler : ClusterEventHandler<UpscaleAmbariRequest> {

    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val clusterUpscaleService: AmbariClusterUpscaleService? = null

    override fun type(): Class<UpscaleAmbariRequest> {
        return UpscaleAmbariRequest::class.java
    }

    override fun accept(event: Event<UpscaleAmbariRequest>) {
        val request = event.data
        val result: UpscaleAmbariResult
        try {
            clusterUpscaleService!!.upscaleAmbari(request.stackId, request.hostGroupName,
                    request.scalingAdjustment)
            result = UpscaleAmbariResult(request)
        } catch (e: Exception) {
            result = UpscaleAmbariResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
