package com.sequenceiq.periscope.controller.mapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class JaxRsNotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsNotFoundExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.NOT_FOUND;
    }

    @Override
    public Response toResponse(NotFoundException exception) {
        String absolutePath = uriInfo.getRequestUri().getPath();
        LOGGER.info("Couldn't find the specified resource on path '{}', error message: {}", absolutePath, exception.getMessage());
        return Response.status(getResponseStatus()).entity(getEntity(exception)).build();
    }
}
