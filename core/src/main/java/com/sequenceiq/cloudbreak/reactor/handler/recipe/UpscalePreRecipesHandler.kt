package com.sequenceiq.cloudbreak.reactor.handler.recipe

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesResult
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class UpscalePreRecipesHandler : ClusterEventHandler<UpscalePreRecipesRequest> {

    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val clusterUpscaleService: AmbariClusterUpscaleService? = null

    override fun type(): Class<UpscalePreRecipesRequest> {
        return UpscalePreRecipesRequest::class.java
    }

    override fun accept(event: Event<UpscalePreRecipesRequest>) {
        val request = event.data
        val result: UpscalePreRecipesResult
        try {
            clusterUpscaleService!!.executePreRecipesOnNewHosts(request.stackId, request.hostGroupName)
            result = UpscalePreRecipesResult(request)
        } catch (e: Exception) {
            result = UpscalePreRecipesResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
