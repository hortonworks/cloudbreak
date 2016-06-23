package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors

import reactor.bus.Event

@Component
class PlatformParameterHandler : CloudPlatformEventHandler<PlatformParameterRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<PlatformParameterRequest> {
        return PlatformParameterRequest::class.java
    }

    override fun accept(platformParameterRequestEvent: Event<PlatformParameterRequest>) {
        LOGGER.info("Received event: {}", platformParameterRequestEvent)
        val request = platformParameterRequestEvent.data
        try {
            val connector = cloudPlatformConnectors!!.get(request.cloudContext.platformVariant)
            val platformParameters = connector.parameters()

            val platformParameterResult = PlatformParameterResult(request, platformParameters)
            request.result.onNext(platformParameterResult)
            LOGGER.info("Query platform parameters finished.")
        } catch (e: Exception) {
            request.result.onNext(PlatformParameterResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PlatformParameterHandler::class.java)
    }
}
