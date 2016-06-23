package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException

import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class DataIntegrityViolationExceptionMapper : ExceptionMapper<DataIntegrityViolationException> {

    override fun toResponse(exception: DataIntegrityViolationException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getLocalizedMessage()).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(DataIntegrityViolationExceptionMapper::class.java)
    }
}
