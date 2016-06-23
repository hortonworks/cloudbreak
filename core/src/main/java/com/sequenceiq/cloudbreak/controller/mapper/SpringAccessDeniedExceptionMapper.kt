package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class SpringAccessDeniedExceptionMapper : ExceptionMapper<AccessDeniedException> {

    override fun toResponse(exception: AccessDeniedException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.FORBIDDEN).entity(ExceptionResult(exception.message)).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SpringAccessDeniedExceptionMapper::class.java)
    }
}
