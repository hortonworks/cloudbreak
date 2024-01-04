package com.sequenceiq.periscope.controller.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.springframework.web.HttpMediaTypeNotSupportedException;

@Provider
public class HttpMediaTypeNotSupportedExceptionMapper extends BaseExceptionMapper<HttpMediaTypeNotSupportedException> {

    @Override
    protected Object getEntity(HttpMediaTypeNotSupportedException exception) {
        return exception.getMessage();
    }

    @Override
    Status getResponseStatus() {
        return Status.NOT_ACCEPTABLE;
    }
}
