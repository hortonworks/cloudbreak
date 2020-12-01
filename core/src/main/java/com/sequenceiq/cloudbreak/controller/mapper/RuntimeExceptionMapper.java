package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;

@Component
public class RuntimeExceptionMapper extends BaseExceptionMapper<RuntimeException> {

    @Override
    protected Object getEntity(RuntimeException exception) {
        return new ExceptionResponse("Internal server error: " + exception.getMessage());
    }

    @Override
    Status getResponseStatus(RuntimeException exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    Class<RuntimeException> getExceptionType() {
        return RuntimeException.class;
    }
}