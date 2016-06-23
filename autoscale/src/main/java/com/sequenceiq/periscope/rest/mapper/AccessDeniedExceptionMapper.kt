package com.sequenceiq.periscope.rest.mapper

import java.nio.file.AccessDeniedException

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.periscope.api.model.ExceptionResult

@Provider
class AccessDeniedExceptionMapper : ExceptionMapper<AccessDeniedException> {

    override fun toResponse(exception: AccessDeniedException): Response {
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.FORBIDDEN).entity(ExceptionResult(exception.message)).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AccessDeniedExceptionMapper::class.java)
    }
}
