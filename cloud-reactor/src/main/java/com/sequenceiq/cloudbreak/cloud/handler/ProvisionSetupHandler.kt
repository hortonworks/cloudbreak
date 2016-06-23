package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ProvisionSetupHandler : CloudPlatformEventHandler<SetupRequest<Any>> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    @Inject
    private val resourceNotifier: ResourceNotifier? = null

    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<SetupRequest<Any>> {
        return SetupRequest<Any>::class.java
    }

    override fun accept(event: Event<SetupRequest<Any>>) {
        LOGGER.info("Received event: {}", event)
        val request = event.data
        val cloudContext = request.cloudContext
        try {
            val connector = cloudPlatformConnectors!!.get(cloudContext.platformVariant)
            val auth = connector.authentication().authenticate(cloudContext, request.cloudCredential)
            val cloudStack = request.cloudStack
            connector.setup().prerequisites(auth, cloudStack, resourceNotifier)

            val result = SetupResult(request)
            request.result.onNext(result)
            eventBus!!.notify(result.selector(), Event(event.headers, result))
            LOGGER.info("Provision setup finished for {}", cloudContext)
        } catch (e: Exception) {
            val failure = SetupResult(e, request)
            request.result.onNext(failure)
            eventBus!!.notify(failure.selector(), Event(event.headers, failure))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ProvisionSetupHandler::class.java)
    }
}
