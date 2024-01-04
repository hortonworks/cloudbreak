package com.sequenceiq.periscope.controller.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class WebApplicaitonExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicaitonExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        String absolutePath = uriInfo.getRequestUri().getPath();
        LOGGER.info("Failed to process request on path '{}', error message: {}", absolutePath, exception.getMessage());
        return exception.getResponse();
    }
}
