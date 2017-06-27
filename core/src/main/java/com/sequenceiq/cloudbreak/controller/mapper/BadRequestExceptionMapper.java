package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper extends SendNotificationExceptionMapper<BadRequestException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}