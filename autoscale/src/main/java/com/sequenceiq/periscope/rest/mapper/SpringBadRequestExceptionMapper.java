package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class SpringBadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}