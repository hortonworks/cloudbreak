package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class IllegalArgumentExceptionMapper extends SendNotificationExceptionMapper<IllegalArgumentException> {

    @Override
    public Status getResponseStatus(IllegalArgumentException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<IllegalArgumentException> getExceptionType() {
        return IllegalArgumentException.class;
    }
}
