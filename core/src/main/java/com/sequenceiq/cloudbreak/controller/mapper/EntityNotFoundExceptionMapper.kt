package com.sequenceiq.cloudbreak.controller.mapper

import javax.persistence.EntityNotFoundException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult
import com.sequenceiq.cloudbreak.logger.MDCBuilder

@Provider
class EntityNotFoundExceptionMapper : ExceptionMapper<EntityNotFoundException> {

    override fun toResponse(exception: EntityNotFoundException): Response {
        MDCBuilder.buildMdcContext()
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.NOT_FOUND).entity(ExceptionResult(exception.message)).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(EntityNotFoundExceptionMapper::class.java)
    }
}
