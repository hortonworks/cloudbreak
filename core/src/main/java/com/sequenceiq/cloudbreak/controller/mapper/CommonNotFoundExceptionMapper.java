package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class CommonNotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonNotFoundExceptionMapper.class);

    @Override
    Status getResponseStatus() {
        return Status.NOT_FOUND;
    }

    @Override
    Class<NotFoundException> getExceptionType() {
        return NotFoundException.class;
    }

    @Override
    public Response toResponse(NotFoundException exception) {
        LOGGER.info("Resource not found: {}", getErrorMessage(exception));
        return Response.status(getResponseStatus()).entity(getEntity(exception)).build();
    }
}
