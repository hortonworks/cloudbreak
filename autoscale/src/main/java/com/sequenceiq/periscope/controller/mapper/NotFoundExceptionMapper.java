package com.sequenceiq.periscope.controller.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.periscope.service.NotFoundException;

@Provider
public class NotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    Status getResponseStatus() {
        return Status.NOT_FOUND;
    }

    @Override
    public Response toResponse(NotFoundException exception) {
        LOGGER.info("Resource not found: {}", getErrorMessage(exception));
        return Response.status(getResponseStatus()).entity(getEntity(exception)).build();
    }

    private String getErrorMessage(NotFoundException exception) {
        String message = exception.getMessage();
        LOGGER.debug("Exception text has been mapped: {}", message);
        return message;
    }
}
