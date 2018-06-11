package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;

@Component
public class HttpMediaTypeNotSupportedExceptionMapper extends BaseExceptionMapper<HttpMediaTypeNotSupportedException> {

    @Override
    Status getResponseStatus() {
        return Status.NOT_ACCEPTABLE;
    }

    @Override
    public Class<HttpMediaTypeNotSupportedException> supportedType() {
        return HttpMediaTypeNotSupportedException.class;
    }
}
