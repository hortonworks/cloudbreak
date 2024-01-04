package com.sequenceiq.environment.exception.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

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
