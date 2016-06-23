package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class RemoveInstanceHandler : CloudPlatformEventHandler<RemoveInstanceRequest<Any>> {

    @Inject
    private val eventBus: EventBus? = null
    @Inject
    @Qualifier("DownscaleStackHandler")
    private val downscaleStackExecuter: DownscaleStackExecuter? = null

    override fun type(): Class<RemoveInstanceRequest<Any>> {
        return RemoveInstanceRequest<Any>::class.java
    }

    override fun accept(removeInstanceRequestEvent: Event<RemoveInstanceRequest<Any>>) {
        val request = removeInstanceRequestEvent.data
        val result: RemoveInstanceResult
        try {
            val downScaleResult = downscaleStackExecuter!!.execute(request)
            result = RemoveInstanceResult(downScaleResult, request)
        } catch (e: Exception) {
            result = RemoveInstanceResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(removeInstanceRequestEvent.headers, result))
    }

}
