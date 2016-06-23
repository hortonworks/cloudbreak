package com.sequenceiq.cloudbreak.controller.mapper

import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

import javax.ws.rs.ext.ExceptionMapper
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class HibernateConstraintViolationException : ExceptionMapper<ConstraintViolationException> {

    override fun toResponse(exception: ConstraintViolationException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getLocalizedMessage()).build()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ConstraintViolationExceptionMapper::class.java)
    }
}
