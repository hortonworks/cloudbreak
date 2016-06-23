package com.sequenceiq.cloudbreak.service.stack.resource.definition

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionResult
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException

import reactor.bus.Event
import reactor.bus.EventBus

@Service
class ResourceDefinitionService {

    @Inject
    private val eventBus: EventBus? = null

    fun getResourceDefinition(cloudPlatform: String, resource: String): String {
        LOGGER.debug("Sending request for {} {} resource property definition", cloudPlatform, resource)
        val platformVariant = CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.EMPTY)
        val request = ResourceDefinitionRequest(platformVariant, resource)
        eventBus!!.notify(request.selector(), Event.wrap(request))
        try {
            val result = request.await()
            LOGGER.info("Resource property definition: {}", result)
            return result.definition
        } catch (e: InterruptedException) {
            LOGGER.error("Error while sending resource definition request", e)
            throw OperationException(e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ResourceDefinitionService::class.java)
    }

}
