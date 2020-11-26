package com.sequenceiq.cloudbreak.controller.mapper;

import static ch.qos.logback.classic.Level.INFO;

import javax.ws.rs.core.Response.Status;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

@Component
public class AuthenticationCredentialsNotFoundExceptionMapper extends BaseExceptionMapper<AuthenticationCredentialsNotFoundException> {

    @Override
    protected Level getLogLevel() {
        return INFO;
    }

    @Override
    Status getResponseStatus(AuthenticationCredentialsNotFoundException exception) {
        return Status.UNAUTHORIZED;
    }

    @Override
    Class<AuthenticationCredentialsNotFoundException> getExceptionType() {
        return AuthenticationCredentialsNotFoundException.class;
    }
}
