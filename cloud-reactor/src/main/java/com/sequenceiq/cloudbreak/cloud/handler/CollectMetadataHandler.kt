package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class CollectMetadataHandler : CloudPlatformEventHandler<CollectMetadataRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<CollectMetadataRequest> {
        return CollectMetadataRequest::class.java
    }

    override fun accept(collectMetadataRequestEvent: Event<CollectMetadataRequest>) {
        LOGGER.info("Received event: {}", collectMetadataRequestEvent)
        val request = collectMetadataRequestEvent.data
        try {
            val connector = cloudPlatformConnectors!!.get(request.cloudContext.platformVariant)
            val ac = connector.authentication().authenticate(request.cloudContext, request.cloudCredential)
            val instanceStatuses = connector.metadata().collect(ac, request.cloudResource, request.vms)
            val collectMetadataResult = CollectMetadataResult(request, instanceStatuses)
            request.result.onNext(collectMetadataResult)
            eventBus!!.notify(collectMetadataResult.selector(), Event(collectMetadataRequestEvent.headers, collectMetadataResult))
            LOGGER.info("Metadata collection successfully finished")
        } catch (e: Exception) {
            val failure = CollectMetadataResult(e, request)
            request.result.onNext(failure)
            eventBus!!.notify(failure.selector(), Event(collectMetadataRequestEvent.headers, failure))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CollectMetadataHandler::class.java)
    }
}
