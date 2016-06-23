package com.sequenceiq.cloudbreak.service.stack.connector.adapter

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.image.ImageService
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ServiceProviderSetupAdapter {

    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val credentialConverter: CredentialToCloudCredentialConverter? = null
    @Inject
    private val imageService: ImageService? = null
    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null

    @Throws(Exception::class)
    fun checkImage(stack: Stack): ImageStatusResult {
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val image = imageService!!.getImage(stack.id)
        val checkImageRequest = CheckImageRequest<CheckImageResult>(cloudContext, cloudCredential, cloudStackConverter!!.convert(stack), image)
        LOGGER.info("Triggering event: {}", checkImageRequest)
        eventBus!!.notify(checkImageRequest.selector(), Event.wrap(checkImageRequest))
        try {
            val res = checkImageRequest.await()
            LOGGER.info("Result: {}", res)
            if (res.errorDetails != null) {
                LOGGER.error("Failed to check image state", res.errorDetails)
                throw OperationException(res.errorDetails)
            }
            return ImageStatusResult(res.imageStatus, res.statusProgressValue)
        } catch (e: InterruptedException) {
            LOGGER.error("Error while executing check image", e)
            throw OperationException(e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ServiceProviderSetupAdapter::class.java)
    }
}
