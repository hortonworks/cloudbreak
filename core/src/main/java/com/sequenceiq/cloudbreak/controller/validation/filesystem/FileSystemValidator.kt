package com.sequenceiq.cloudbreak.controller.validation.filesystem

import java.io.IOException

import javax.inject.Inject
import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationRequest
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationResult
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.converter.spi.FileSystemRequestToFileSystemConverter
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException
import com.sequenceiq.cloudbreak.util.JsonUtil

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class FileSystemValidator {

    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val converter: FileSystemRequestToFileSystemConverter? = null

    fun validateFileSystem(platform: String, fileSystemRequest: FileSystemRequest?) {
        if (fileSystemRequest == null) {
            return
        }
        validateFilesystemRequest(fileSystemRequest)
        LOGGER.debug("Sending fileSystemRequest to {} to validate the file system", platform)
        val cloudContext = CloudContext(null, null, platform, null, null, null)
        val fileSystem = converter!!.convert(fileSystemRequest)
        val request = FileSystemValidationRequest(fileSystem, cloudContext)
        eventBus!!.notify(request.selector(), Event.wrap(request))
        try {
            val result = request.await()
            LOGGER.info("File system validation result: {}", result)
            val exception = result.errorDetails
            if (exception != null) {
                throw BadRequestException(result.statusReason, exception)
            }
        } catch (e: InterruptedException) {
            LOGGER.error("Error while sending the file system validation request", e)
            throw OperationException(e)
        }

    }

    private fun validateFilesystemRequest(fileSystemRequest: FileSystemRequest) {
        val validatorFactory = Validation.buildDefaultValidatorFactory()
        val validator = validatorFactory.validator
        try {
            val json = JsonUtil.writeValueAsString(fileSystemRequest.properties)
            val fsConfig = JsonUtil.readValue(json, fileSystemRequest.type.clazz)
            val violations = validator.validate(fsConfig)
            if (!violations.isEmpty()) {
                throw ConstraintViolationException(violations)
            }
        } catch (e: IOException) {
            throw BadRequestException(e.message, e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(FileSystemValidator::class.java)
    }

}
