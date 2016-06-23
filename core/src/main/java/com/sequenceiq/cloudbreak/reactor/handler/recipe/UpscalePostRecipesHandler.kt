package com.sequenceiq.cloudbreak.reactor.handler.recipe

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class UpscalePostRecipesHandler : ClusterEventHandler<UpscalePostRecipesRequest> {

    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val clusterUpscaleService: AmbariClusterUpscaleService? = null

    override fun type(): Class<UpscalePostRecipesRequest> {
        return UpscalePostRecipesRequest::class.java
    }

    override fun accept(event: Event<UpscalePostRecipesRequest>) {
        val request = event.data
        val result: UpscalePostRecipesResult
        try {
            clusterUpscaleService!!.executePostRecipesOnNewHosts(request.stackId, request.hostGroupName)
            result = UpscalePostRecipesResult(request)
        } catch (e: Exception) {
            result = UpscalePostRecipesResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
