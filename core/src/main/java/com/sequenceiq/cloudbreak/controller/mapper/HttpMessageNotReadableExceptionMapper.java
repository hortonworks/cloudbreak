package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

@Component
public class HttpMessageNotReadableExceptionMapper extends BaseExceptionMapper<HttpMessageNotReadableException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<HttpMessageNotReadableException> supportedType() {
        return HttpMessageNotReadableException.class;
    }
}