package com.sequenceiq.periscope.rest.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.converter.HttpMessageNotReadableException

import com.sequenceiq.periscope.api.model.ExceptionResult

@Provider
class HttpMessageNotReadableExceptionMapper : ExceptionMapper<HttpMessageNotReadableException> {

    override fun toResponse(exception: HttpMessageNotReadableException): Response {
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.BAD_REQUEST).entity(ExceptionResult(exception.message)).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HttpMessageNotReadableExceptionMapper::class.java)
    }
}