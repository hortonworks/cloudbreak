package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

@Component
public class IllegalArgumentExceptionMapper extends SendNotificationExceptionMapper<IllegalArgumentException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<IllegalArgumentException> getExceptionType() {
        return IllegalArgumentException.class;
    }

    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }
}
