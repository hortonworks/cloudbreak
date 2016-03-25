package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.periscope.api.model.ExceptionResult;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ExceptionResult("Internal server error"))
                .build();
    }
}
