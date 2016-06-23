package com.sequenceiq.periscope.rest.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.periscope.api.model.ExceptionResult

@Provider
class RuntimeExceptionMapper : ExceptionMapper<RuntimeException> {

    override fun toResponse(exception: RuntimeException): Response {
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionResult("Internal server error")).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(RuntimeExceptionMapper::class.java)
    }
}