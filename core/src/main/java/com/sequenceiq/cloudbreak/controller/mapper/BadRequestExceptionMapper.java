package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper extends SendNotificationExceptionMapper<BadRequestException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}