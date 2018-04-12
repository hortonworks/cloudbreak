package com.sequenceiq.cloudbreak.controller.mapper;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthenticationCredentialsNotFoundExceptionMapper extends BaseExceptionMapper<AuthenticationCredentialsNotFoundException> {

    @Override
    Status getResponseStatus() {
        return Status.UNAUTHORIZED;
    }
}
