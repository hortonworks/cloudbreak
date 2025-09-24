package com.sequenceiq.cloudbreak.exception.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

@Component
@Provider
public class DefaultExceptionMapper extends BaseExceptionMapper<Exception> {

    @Override
    protected String getErrorMessage(Exception exception) {
        return "Default error handler: " + super.getErrorMessage(exception);
    }

    @Override
    public Status getResponseStatus(Exception exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    public Class<Exception> getExceptionType() {
        return Exception.class;
    }
}
