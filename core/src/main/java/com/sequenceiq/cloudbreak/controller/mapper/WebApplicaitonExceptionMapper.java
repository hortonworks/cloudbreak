package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicaitonExceptionMapper extends BaseExceptionMapper<WebApplicationException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    Class<WebApplicationException> getExceptionType() {
        return WebApplicationException.class;
    }

}
