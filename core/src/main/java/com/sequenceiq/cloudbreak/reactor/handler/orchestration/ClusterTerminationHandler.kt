package com.sequenceiq.cloudbreak.reactor.handler.orchestration

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService
import com.sequenceiq.cloudbreak.service.stack.StackService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ClusterTerminationHandler : ClusterEventHandler<ClusterTerminationRequest> {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val clusterTerminationService: ClusterTerminationService? = null

    override fun type(): Class<ClusterTerminationRequest> {
        return ClusterTerminationRequest::class.java
    }

    override fun accept(event: Event<ClusterTerminationRequest>) {
        val request = event.data
        val result: ClusterTerminationResult
        try {
            clusterTerminationService!!.deleteClusterContainers(request.clusterId)
            result = ClusterTerminationResult(request)
        } catch (e: Exception) {
            LOGGER.error("Failed to delete cluster containers: {}", e)
            result = ClusterTerminationResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterTerminationHandler::class.java)
    }
}
