package com.sequenceiq.periscope.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;

@Provider
public class RuntimeExceptionMapper extends BaseExceptionMapper<RuntimeException> {

    @Override
    protected Object getEntity(RuntimeException exception) {
        return new ExceptionResponse("Internal server error");
    }

    @Override
    Status getResponseStatus() {
        return Status.INTERNAL_SERVER_ERROR;
    }
}