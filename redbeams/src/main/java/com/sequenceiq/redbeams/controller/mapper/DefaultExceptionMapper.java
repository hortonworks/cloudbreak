package com.sequenceiq.redbeams.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.redbeams.api.model.ExceptionResponse;

@Provider
public class DefaultExceptionMapper extends BaseExceptionMapper<Exception> {

    @Override
    protected Object getEntity(Exception exception) {
        return new ExceptionResponse("Internal server error: " + exception.getMessage());
    }

    @Override
    Status getResponseStatus() {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    Class<Exception> getExceptionType() {
        return Exception.class;
    }
}
