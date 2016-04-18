package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpMediaTypeNotSupportedException;

@Provider
public class HttpMediaTypeNotSupportedExceptionMapper implements ExceptionMapper<HttpMediaTypeNotSupportedException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpMediaTypeNotSupportedExceptionMapper.class);

    @Override
    public Response toResponse(HttpMediaTypeNotSupportedException exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(exception.getMessage()).build();
    }
}
