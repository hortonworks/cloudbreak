package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class RuntimeExceptionMapper extends BaseExceptionMapper<RuntimeException> {

    @Override
    protected Object getEntity(RuntimeException exception) {
        return new ExceptionResponse("Internal server error: " + exception.getMessage());
    }

    @Override
    public Status getResponseStatus(RuntimeException exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    public Class<RuntimeException> getExceptionType() {
        return RuntimeException.class;
    }
}