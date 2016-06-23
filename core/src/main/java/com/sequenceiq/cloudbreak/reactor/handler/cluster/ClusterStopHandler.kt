package com.sequenceiq.cloudbreak.reactor.handler.cluster

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector
import com.sequenceiq.cloudbreak.service.stack.StackService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ClusterStopHandler : ClusterEventHandler<ClusterStopRequest> {
    @Inject
    private val ambariClusterConnector: AmbariClusterConnector? = null
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<ClusterStopRequest> {
        return ClusterStopRequest::class.java
    }

    override fun accept(event: Event<ClusterStopRequest>) {
        val request = event.data
        val result: ClusterStopResult
        try {
            val stack = stackService!!.getById(request.stackId)
            ambariClusterConnector!!.stopCluster(stack)
            result = ClusterStopResult(request)
        } catch (e: Exception) {
            result = ClusterStopResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
