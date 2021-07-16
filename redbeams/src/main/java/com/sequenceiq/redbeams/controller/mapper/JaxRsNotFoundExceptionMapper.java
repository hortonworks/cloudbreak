package com.sequenceiq.redbeams.controller.mapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class JaxRsNotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsNotFoundExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Override
    public Response.Status getResponseStatus(NotFoundException exception) {
        return Response.Status.NOT_FOUND;
    }

    @Override
    public Class<NotFoundException> getExceptionType() {
        return NotFoundException.class;
    }

    @Override
    public Response toResponse(NotFoundException exception) {
        String absolutePath = uriInfo.getRequestUri().getPath();
        LOGGER.info("Couldn't find the specified resource on path '{}', error message: {}", absolutePath, getErrorMessage(exception));
        return Response.status(getResponseStatus(exception)).entity(getEntity(exception)).build();
    }
}
