package com.sequenceiq.environment.exception.mapper;

import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

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
