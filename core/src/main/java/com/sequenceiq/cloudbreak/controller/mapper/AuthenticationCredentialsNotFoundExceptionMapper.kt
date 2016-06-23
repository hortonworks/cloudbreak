package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class AuthenticationCredentialsNotFoundExceptionMapper : ExceptionMapper<AuthenticationCredentialsNotFoundException> {

    override fun toResponse(exception: AuthenticationCredentialsNotFoundException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.UNAUTHORIZED).entity(ExceptionResult(exception.message)).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AuthenticationCredentialsNotFoundExceptionMapper::class.java)
    }
}
