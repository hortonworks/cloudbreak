package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Provider
public class DuplicatedKeyValueExceptionMapper implements ExceptionMapper<DuplicateKeyValueException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatedKeyValueExceptionMapper.class);

    @Override
    public Response toResponse(DuplicateKeyValueException exception) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.CONFLICT).entity(new ExceptionResult(
                String.format("The %s name '%s' is already taken, please choose a different one",
                        exception.getResourceType().toString().toLowerCase(),
                        exception.getValue())))
                .build();
    }
}
