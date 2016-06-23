package com.sequenceiq.cloudbreak.reactor

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.status.AmbariClusterStatusUpdater
import com.sequenceiq.cloudbreak.service.stack.StackService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ClusterSyncHandler : ClusterEventHandler<ClusterSyncRequest> {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val ambariClusterStatusUpdater: AmbariClusterStatusUpdater? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<ClusterSyncRequest> {
        return ClusterSyncRequest::class.java
    }

    override fun accept(event: Event<ClusterSyncRequest>) {
        val request = event.data
        val result: ClusterSyncResult
        try {
            val stack = stackService!!.getById(request.stackId)
            val cluster = clusterService!!.retrieveClusterByStackId(request.stackId)
            ambariClusterStatusUpdater!!.updateClusterStatus(stack, cluster)
            result = ClusterSyncResult(request)
        } catch (e: Exception) {
            result = ClusterSyncResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
