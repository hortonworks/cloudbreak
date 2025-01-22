package com.sequenceiq.environment.exception.mapper;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class ForbiddenExceptionMapper extends EnvironmentBaseExceptionMapper<ForbiddenException> {

    @Override
    public Status getResponseStatus(ForbiddenException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<ForbiddenException> getExceptionType() {
        return ForbiddenException.class;
    }
}
