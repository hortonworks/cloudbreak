package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

@Provider
public class AuthenticationCredentialsNotFoundExceptionMapper extends BaseExceptionMapper<AuthenticationCredentialsNotFoundException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.UNAUTHORIZED;
    }
}
