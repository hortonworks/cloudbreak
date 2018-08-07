package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationCredentialsNotFoundExceptionMapper extends BaseExceptionMapper<AuthenticationCredentialsNotFoundException> {

    @Override
    Status getResponseStatus() {
        return Status.UNAUTHORIZED;
    }

    @Override
    Class<AuthenticationCredentialsNotFoundException> getExceptionType() {
        return AuthenticationCredentialsNotFoundException.class;
    }
}
