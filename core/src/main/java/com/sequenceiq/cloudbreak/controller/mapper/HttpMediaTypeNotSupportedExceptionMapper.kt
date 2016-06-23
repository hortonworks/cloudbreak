package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.HttpMediaTypeNotSupportedException

import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class HttpMediaTypeNotSupportedExceptionMapper : ExceptionMapper<HttpMediaTypeNotSupportedException> {

    override fun toResponse(exception: HttpMediaTypeNotSupportedException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(exception.message).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HttpMediaTypeNotSupportedExceptionMapper::class.java)
    }
}
