package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory
import com.sequenceiq.cloudbreak.common.type.ImageStatus
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class CheckImageHandler : CloudPlatformEventHandler<CheckImageRequest<Any>> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val statusCheckFactory: PollTaskFactory? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<CheckImageRequest<Any>> {
        return CheckImageRequest<Any>::class.java
    }

    override fun accept(event: Event<CheckImageRequest<Any>>) {
        LOGGER.info("Received event: {}", event)
        val request = event.data
        val cloudContext = request.cloudContext
        try {
            val connector = cloudPlatformConnectors!!.get(request.cloudContext.platformVariant)
            val auth = connector.authentication().authenticate(cloudContext, request.cloudCredential)
            val image = request.image
            val stack = request.stack
            val progress = connector.setup().checkImageStatus(auth, stack, image)
            val imageResult = CheckImageResult(request, progress.imageStatus, progress.statusProgressValue)
            request.result.onNext(imageResult)
            LOGGER.info("Provision setup finished for {}", cloudContext)
        } catch (e: Exception) {
            val failure = CheckImageResult(e, request, ImageStatus.CREATE_FAILED)
            request.result.onNext(failure)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CheckImageHandler::class.java)
    }
}
