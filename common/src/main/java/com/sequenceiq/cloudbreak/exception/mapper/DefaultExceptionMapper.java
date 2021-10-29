package com.sequenceiq.cloudbreak.exception.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultExceptionMapper extends BaseExceptionMapper<Exception> {

    @Override
    protected String getErrorMessage(Exception exception) {
        return "Default error handler: " + exception.getMessage();
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
