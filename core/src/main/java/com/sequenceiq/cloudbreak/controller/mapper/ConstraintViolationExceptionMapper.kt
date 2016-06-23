package com.sequenceiq.cloudbreak.controller.mapper

import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.controller.json.ValidationResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class ConstraintViolationExceptionMapper : ExceptionMapper<ConstraintViolationException> {

    override fun toResponse(exception: ConstraintViolationException): Response {
        MDCBuilder.buildMdcContext()
        val result = ValidationResult()
        for (violation in exception.constraintViolations) {
            var key = ""
            if (violation.propertyPath != null) {
                key = violation.propertyPath.toString()
            }
            result.addValidationError(key, violation.message)
        }
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.BAD_REQUEST).entity(result).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ConstraintViolationExceptionMapper::class.java)
    }
}
