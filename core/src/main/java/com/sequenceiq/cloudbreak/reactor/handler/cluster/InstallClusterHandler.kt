package com.sequenceiq.cloudbreak.reactor.handler.cluster

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterCreationService
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterFailed
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class InstallClusterHandler : ReactorEventHandler<InstallClusterRequest> {
    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val ambariClusterCreationService: AmbariClusterCreationService? = null

    override fun selector(): String {
        return EventSelectorUtil.selector(InstallClusterRequest::class.java)
    }

    override fun accept(event: Event<InstallClusterRequest>) {
        val stackId = event.data.stackId
        val response: Selectable
        try {
            ambariClusterCreationService!!.buildAmbariCluster(stackId)
            response = InstallClusterSuccess(stackId)
        } catch (e: Exception) {
            response = InstallClusterFailed(stackId, e)
        }

        eventBus!!.notify(response.selector(), Event(event.headers, response))
    }
}
