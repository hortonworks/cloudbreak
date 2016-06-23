package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Provider
class DuplicatedKeyValueExceptionMapper : ExceptionMapper<DuplicateKeyValueException> {

    override fun toResponse(exception: DuplicateKeyValueException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.CONFLICT).entity(ExceptionResult(
                String.format("The %s name '%s' is already taken, please choose a different one",
                        exception.resourceType.toString().toLowerCase(),
                        exception.value))).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(DuplicatedKeyValueExceptionMapper::class.java)
    }
}
