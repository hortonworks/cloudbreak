package com.sequenceiq.cloudbreak.reactor

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataResult
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterDownscaleService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class UpdateInstanceMetadataHandler : ClusterEventHandler<UpdateInstanceMetadataRequest> {

    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val clusterDownscaleService: ClusterDownscaleService? = null

    override fun type(): Class<UpdateInstanceMetadataRequest> {
        return UpdateInstanceMetadataRequest::class.java
    }

    override fun accept(event: Event<UpdateInstanceMetadataRequest>) {
        val request = event.data
        val result: UpdateInstanceMetadataResult
        try {
            clusterDownscaleService!!.updateMetadata(request.stackId, request.hostNames)
            result = UpdateInstanceMetadataResult(request)
        } catch (e: Exception) {
            result = UpdateInstanceMetadataResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
