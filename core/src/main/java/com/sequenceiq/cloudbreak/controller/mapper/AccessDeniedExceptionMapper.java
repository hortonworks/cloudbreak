package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

@Component
public class AccessDeniedExceptionMapper extends BaseExceptionMapper<AccessDeniedException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }

    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }

    @Override
    Class<AccessDeniedException> getExceptionType() {
        return AccessDeniedException.class;
    }
}
