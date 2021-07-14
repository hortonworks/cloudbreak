package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AccessDeniedExceptionMapper extends EnvironmentBaseExceptionMapper<AccessDeniedException> {

    @Override
    public Status getResponseStatus(AccessDeniedException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<AccessDeniedException> getExceptionType() {
        return AccessDeniedException.class;
    }
}
