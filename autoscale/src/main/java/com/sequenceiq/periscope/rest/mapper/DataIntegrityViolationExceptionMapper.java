package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

@Provider
public class DataIntegrityViolationExceptionMapper implements ExceptionMapper<DataIntegrityViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataIntegrityViolationExceptionMapper.class);

    @Override
    public Response toResponse(DataIntegrityViolationException exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getLocalizedMessage())
                .build();
    }
}
