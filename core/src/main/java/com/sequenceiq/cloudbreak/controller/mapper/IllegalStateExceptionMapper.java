package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class IllegalStateExceptionMapper extends SendNotificationExceptionMapper<IllegalStateException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<IllegalStateException> getExceptionType() {
        return IllegalStateException.class;
    }
}
