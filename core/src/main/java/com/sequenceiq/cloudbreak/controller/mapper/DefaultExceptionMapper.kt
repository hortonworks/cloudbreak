package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class DefaultExceptionMapper : ExceptionMapper<Exception> {

    override fun toResponse(exception: Exception): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionResult("Internal server error")).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(DefaultExceptionMapper::class.java)
    }
}
