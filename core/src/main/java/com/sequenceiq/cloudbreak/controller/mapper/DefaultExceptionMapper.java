package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultExceptionMapper extends BaseExceptionMapper<Exception> {

    @Override
    protected Object getEntity(Exception exception) {
        return new ExceptionResult("Internal server error: " + exception.getMessage());
    }

    @Override
    Status getResponseStatus() {
        return Status.INTERNAL_SERVER_ERROR;
    }
}
