package com.sequenceiq.periscope.rest.mapper;

import java.text.ParseException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ParseExceptionMapper implements ExceptionMapper<ParseException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseException.class);

    @Override
    public Response toResponse(ParseException exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage())
                .build();
    }

}
