package com.sequenceiq.cloudbreak.reactor.handler.cluster

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector
import com.sequenceiq.cloudbreak.service.stack.StackService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ClusterStartHandler : ClusterEventHandler<ClusterStartRequest> {
    @Inject
    private val ambariClusterConnector: AmbariClusterConnector? = null
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<ClusterStartRequest> {
        return ClusterStartRequest::class.java
    }

    override fun accept(event: Event<ClusterStartRequest>) {
        val request = event.data
        val result: ClusterStartResult
        try {
            val stack = stackService!!.getById(request.stackId)
            ambariClusterConnector!!.startCluster(stack)
            result = ClusterStartResult(request)
        } catch (e: Exception) {
            result = ClusterStartResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
