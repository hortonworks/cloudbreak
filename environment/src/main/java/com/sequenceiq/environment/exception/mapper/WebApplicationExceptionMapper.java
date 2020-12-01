package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class WebApplicationExceptionMapper extends BaseExceptionMapper<WebApplicationException> {

    @Override
    Response.Status getResponseStatus(WebApplicationException exception) {
        return Response.Status.fromStatusCode(exception.getResponse().getStatus());
    }

    @Override
    Class<WebApplicationException> getExceptionType() {
        return WebApplicationException.class;
    }

}
