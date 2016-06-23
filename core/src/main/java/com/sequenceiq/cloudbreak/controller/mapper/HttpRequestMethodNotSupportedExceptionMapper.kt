package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.HttpRequestMethodNotSupportedException

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class HttpRequestMethodNotSupportedExceptionMapper : ExceptionMapper<HttpRequestMethodNotSupportedException> {

    override fun toResponse(exception: HttpRequestMethodNotSupportedException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.BAD_REQUEST).entity(ExceptionResult("The requested http method is not supported on the resource.")).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HttpRequestMethodNotSupportedExceptionMapper::class.java)
    }
}
