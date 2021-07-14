package com.sequenceiq.datalake.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Provider
@Component
public class SpringNotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringNotFoundExceptionMapper.class);

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
