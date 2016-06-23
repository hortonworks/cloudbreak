package com.sequenceiq.cloudbreak.reactor.handler.orchestration

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult
import com.sequenceiq.cloudbreak.service.stack.flow.HostMetadataSetup

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ExtendHostMetadataHandler : ClusterEventHandler<ExtendHostMetadataRequest> {
    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val hostMetadataSetup: HostMetadataSetup? = null

    override fun type(): Class<ExtendHostMetadataRequest> {
        return ExtendHostMetadataRequest::class.java
    }

    override fun accept(event: Event<ExtendHostMetadataRequest>) {
        val request = event.data
        val result: ExtendHostMetadataResult
        try {
            hostMetadataSetup!!.setupNewHostMetadata(request.stackId, request.upscaleCandidateAddresses)
            result = ExtendHostMetadataResult(request)
        } catch (e: Exception) {
            result = ExtendHostMetadataResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))

    }
}
