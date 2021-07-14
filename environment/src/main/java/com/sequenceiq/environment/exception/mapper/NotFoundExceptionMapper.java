package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class NotFoundExceptionMapper extends EnvironmentBaseExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    public Status getResponseStatus(NotFoundException exception) {
        return Status.NOT_FOUND;
    }

    @Override
    public Class<NotFoundException> getExceptionType() {
        return NotFoundException.class;
    }

    @Override
    public Response toResponse(NotFoundException exception) {
        LOGGER.info("Resource not found: {}", getErrorMessage(exception));
        return Response.status(getResponseStatus(exception)).entity(getEntity(exception)).build();
    }
}
