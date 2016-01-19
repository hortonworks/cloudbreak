package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Provider
public class DataIntegrityViolationExceptionMapper implements ExceptionMapper<DataIntegrityViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataIntegrityViolationExceptionMapper.class);

    @Override
    public Response toResponse(DataIntegrityViolationException exception) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getLocalizedMessage())
                .build();
    }
}
