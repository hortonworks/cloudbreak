package com.sequenceiq.environment.exception.mapper;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Priority(1)
@Component
public class DefaultEnvironmentExceptionMapper extends SearchCauseExceptionMapper<Exception> {

    @Override
    public Status getResponseStatus(Exception exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    public Class<Exception> getExceptionType() {
        return Exception.class;
    }

}
