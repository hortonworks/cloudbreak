package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.controller.BadRequestException;

@Provider
public class SpringBadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}