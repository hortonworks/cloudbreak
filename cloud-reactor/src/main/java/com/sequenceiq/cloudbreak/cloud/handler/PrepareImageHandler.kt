package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Image

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class PrepareImageHandler : CloudPlatformEventHandler<PrepareImageRequest<Any>> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<PrepareImageRequest<Any>> {
        return PrepareImageRequest<Any>::class.java
    }

    override fun accept(event: Event<PrepareImageRequest<Any>>) {
        LOGGER.info("Received event: {}", event)
        val request = event.data
        val cloudContext = request.cloudContext
        try {
            val connector = cloudPlatformConnectors!!.get(request.cloudContext.platformVariant)
            val auth = connector.authentication().authenticate(cloudContext, request.cloudCredential)
            val image = request.image
            val stack = request.stack
            connector.setup().prepareImage(auth, stack, image)

            val result = PrepareImageResult(request)
            request.result.onNext(result)
            eventBus!!.notify(result.selector(), Event(event.headers, result))
            LOGGER.info("Prepare image finished for {}", cloudContext)
        } catch (e: Exception) {
            val failure = PrepareImageResult(e, request)
            request.result.onNext(failure)
            eventBus!!.notify(failure.selector(), Event(event.headers, failure))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(PrepareImageHandler::class.java)
    }
}
