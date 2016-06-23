package com.sequenceiq.periscope.rest.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.periscope.api.model.ExceptionResult
import com.sequenceiq.periscope.service.NotFoundException

@Provider
class NotFoundExceptionMapper : ExceptionMapper<NotFoundException> {

    override fun toResponse(exception: NotFoundException): Response {
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.NOT_FOUND).entity(ExceptionResult(exception.message)).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(NotFoundExceptionMapper::class.java)
    }
}
