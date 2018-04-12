package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.web.HttpMediaTypeNotSupportedException;

@Provider
public class HttpMediaTypeNotSupportedExceptionMapper extends BaseExceptionMapper<HttpMediaTypeNotSupportedException> {

    @Override
    Status getResponseStatus() {
        return Status.NOT_ACCEPTABLE;
    }
}
