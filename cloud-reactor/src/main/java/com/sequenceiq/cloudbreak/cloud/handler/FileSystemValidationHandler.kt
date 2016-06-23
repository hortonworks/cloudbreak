package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationRequest
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors

import reactor.bus.Event

@Component
class FileSystemValidationHandler : CloudPlatformEventHandler<FileSystemValidationRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<FileSystemValidationRequest> {
        return FileSystemValidationRequest::class.java
    }

    override fun accept(requestEvent: Event<FileSystemValidationRequest>) {
        LOGGER.info("Received event: {}", requestEvent)
        val request = requestEvent.data
        try {
            val connector = cloudPlatformConnectors!!.get(request.cloudContext.platformVariant)
            connector.setup().validateFileSystem(request.fileSystem)
            request.result.onNext(FileSystemValidationResult(request))
        } catch (e: Exception) {
            request.result.onNext(FileSystemValidationResult(e.message, e, request))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(FileSystemValidationHandler::class.java)
    }
}
