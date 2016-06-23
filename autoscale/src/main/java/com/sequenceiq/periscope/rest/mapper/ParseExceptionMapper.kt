package com.sequenceiq.periscope.rest.mapper

import java.text.ParseException

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Provider
class ParseExceptionMapper : ExceptionMapper<ParseException> {

    override fun toResponse(exception: ParseException): Response {
        LOGGER.error(exception.message, exception)
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.message).build()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ParseException::class.java)
    }

}
