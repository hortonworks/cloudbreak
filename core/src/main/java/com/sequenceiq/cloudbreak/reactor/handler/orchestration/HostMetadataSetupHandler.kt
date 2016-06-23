package com.sequenceiq.cloudbreak.reactor.handler.orchestration

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupFailed
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler
import com.sequenceiq.cloudbreak.service.stack.flow.HostMetadataSetup

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class HostMetadataSetupHandler : ReactorEventHandler<HostMetadataSetupRequest> {
    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val hostMetadataSetup: HostMetadataSetup? = null

    override fun selector(): String {
        return EventSelectorUtil.selector(HostMetadataSetupRequest::class.java)
    }

    override fun accept(event: Event<HostMetadataSetupRequest>) {
        val request = event.data
        val response: Selectable
        try {
            hostMetadataSetup!!.setupHostMetadata(request.stackId)
            response = HostMetadataSetupSuccess(request.stackId)
        } catch (e: Exception) {
            response = HostMetadataSetupFailed(request.stackId, e)
        }

        eventBus!!.notify(response.selector(), Event(event.headers, response))
    }
}
