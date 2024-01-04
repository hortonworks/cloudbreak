package com.sequenceiq.cloudbreak.controller.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class WebApplicationExceptionMapper extends BaseExceptionMapper<WebApplicationException> {

    @Override
    public Response.Status getResponseStatus(WebApplicationException exception) {
        return Response.Status.fromStatusCode(exception.getResponse().getStatus());
    }

    @Override
    public Class<WebApplicationException> getExceptionType() {
        return WebApplicationException.class;
    }

}
