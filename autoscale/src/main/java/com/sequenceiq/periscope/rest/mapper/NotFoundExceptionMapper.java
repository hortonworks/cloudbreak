package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.periscope.api.model.ExceptionResult;
import com.sequenceiq.periscope.service.NotFoundException;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    public Response toResponse(NotFoundException exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.NOT_FOUND).entity(new ExceptionResult(exception.getMessage())).build();
    }
}
