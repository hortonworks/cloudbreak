package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class WebApplicationExceptionMapper extends EnvironmentBaseExceptionMapper<WebApplicationException> {

    @Override
    public Response.Status getResponseStatus(WebApplicationException exception) {
        return Response.Status.fromStatusCode(exception.getResponse().getStatus());
    }

    @Override
    public Class<WebApplicationException> getExceptionType() {
        return WebApplicationException.class;
    }

}
