package com.sequenceiq.periscope.rest.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException

import com.sequenceiq.periscope.api.model.ExceptionResult

@Provider
class SpringAccessDeniedExceptionMapper : ExceptionMapper<AccessDeniedException> {

    override fun toResponse(exception: AccessDeniedException): Response {
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.FORBIDDEN).entity(ExceptionResult(exception.message)).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SpringAccessDeniedExceptionMapper::class.java)
    }
}
