package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors

import reactor.bus.Event

@Component
class ResourceDefinitionHandler : CloudPlatformEventHandler<ResourceDefinitionRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<ResourceDefinitionRequest> {
        return ResourceDefinitionRequest::class.java
    }

    override fun accept(getRegionsRequestEvent: Event<ResourceDefinitionRequest>) {
        LOGGER.info("Received event: {}", getRegionsRequestEvent)
        val request = getRegionsRequestEvent.data
        try {
            val connector = cloudPlatformConnectors!!.get(request.platform)
            val resource = request.resource
            val definition = connector.parameters().resourceDefinition(request.resource)
            if (definition == null) {
                val exception = Exception("Failed to find resource definition for " + resource)
                request.result.onNext(ResourceDefinitionResult(exception.message, exception, request))
            } else {
                request.result.onNext(ResourceDefinitionResult(request, definition))
            }
        } catch (e: Exception) {
            request.result.onNext(ResourceDefinitionResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ResourceDefinitionHandler::class.java)
    }
}
