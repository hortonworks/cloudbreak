package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;

abstract class BaseExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionMapper.class);

    public Response toResponse(E exception) {
        LOGGER.error(getErrorMessage(exception), exception);
        return Response.status(getResponseStatus()).entity(getEntity(exception)).build();
    }

    protected String getErrorMessage(E exception) {
        return exception.getMessage();
    }

    protected Object getEntity(E exception) {
        return new ExceptionResult(exception.getMessage());
    }

    abstract Response.Status getResponseStatus();
}
