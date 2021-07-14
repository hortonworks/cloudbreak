package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class IllegalStateExceptionMapper extends SendNotificationExceptionMapper<IllegalStateException> {

    @Override
    public Status getResponseStatus(IllegalStateException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<IllegalStateException> getExceptionType() {
        return IllegalStateException.class;
    }
}
