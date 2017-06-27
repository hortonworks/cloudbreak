package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.web.HttpMediaTypeNotSupportedException;

@Provider
public class HttpMediaTypeNotSupportedExceptionMapper extends BaseExceptionMapper<HttpMediaTypeNotSupportedException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.NOT_ACCEPTABLE;
    }
}
