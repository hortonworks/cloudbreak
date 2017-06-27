package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.web.HttpMediaTypeNotSupportedException;

@Provider
public class HttpMediaTypeNotSupportedExceptionMapper extends BaseExceptionMapper<HttpMediaTypeNotSupportedException> {

    @Override
    protected Object getEntity(HttpMediaTypeNotSupportedException exception) {
        return exception.getMessage();
    }

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.NOT_ACCEPTABLE;
    }
}
