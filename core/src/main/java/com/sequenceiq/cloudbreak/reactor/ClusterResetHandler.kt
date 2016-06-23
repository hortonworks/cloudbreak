package com.sequenceiq.cloudbreak.reactor

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterResetService
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult
import com.sequenceiq.cloudbreak.service.stack.StackService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ClusterResetHandler : ClusterEventHandler<ClusterResetRequest> {
    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val ambariClusterResetService: AmbariClusterResetService? = null

    override fun type(): Class<ClusterResetRequest> {
        return ClusterResetRequest::class.java
    }

    override fun accept(event: Event<ClusterResetRequest>) {
        val request = event.data
        val result: ClusterResetResult
        try {
            ambariClusterResetService!!.resetCluster(request.stackId)
            result = ClusterResetResult(request)
        } catch (e: Exception) {
            result = ClusterResetResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterResetHandler::class.java)
    }

}
