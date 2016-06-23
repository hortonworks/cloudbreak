package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.BadRequestException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class BadRequestExceptionMapper : ExceptionMapper<BadRequestException> {

    override fun toResponse(exception: BadRequestException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.BAD_REQUEST).entity(ExceptionResult(exception.message)).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(BadRequestExceptionMapper::class.java)
    }
}