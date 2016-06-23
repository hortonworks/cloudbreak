package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException
import org.springframework.validation.FieldError

import com.sequenceiq.cloudbreak.controller.json.ValidationResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class MethodArgumentNotValidExceptionMapper : ExceptionMapper<MethodArgumentNotValidException> {

    override fun toResponse(exception: MethodArgumentNotValidException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        val result = ValidationResult()
        for (err in exception.bindingResult.fieldErrors) {
            result.addValidationError(err.field, err.defaultMessage)
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(result).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(NotFoundExceptionMapper::class.java)
    }
}
