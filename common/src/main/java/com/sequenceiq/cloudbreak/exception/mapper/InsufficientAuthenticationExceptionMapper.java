package com.sequenceiq.cloudbreak.exception.mapper;

import javax.ws.rs.core.Response;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class InsufficientAuthenticationExceptionMapper extends BaseExceptionMapper<InsufficientAuthenticationException> {

    @Override
    public Response.Status getResponseStatus(InsufficientAuthenticationException exception) {
        return Response.Status.FORBIDDEN;
    }

    @Override
    public Class<InsufficientAuthenticationException> getExceptionType() {
        return InsufficientAuthenticationException.class;
    }
}
